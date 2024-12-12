package org.http4k.client

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.ChannelPipeline
import io.netty.channel.EventLoopGroup
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.http4k.core.Body
import org.http4k.core.BodyMode
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CONNECTION_REFUSED
import org.http4k.core.Status.Companion.UNKNOWN_HOST
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.Int.Companion.MAX_VALUE

typealias BootstrapFactory = (EventLoopGroup) -> Bootstrap

class NettyHttpClient(
    private val eventLoopGroup: EventLoopGroup = NioEventLoopGroup(),
    private val bootstrap: BootstrapFactory = defaultBootstrapFactory(),
    private val bodyMode: BodyMode = BodyMode.Memory
) : HttpHandler {

    override fun invoke(request: Request): Response {
        val responsePromise = eventLoopGroup.next().newPromise<Response>()
        val nettyRequest = request.toNettyRequest(bodyMode)

        val connectFuture = bootstrap(eventLoopGroup)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    val pipeline = ch.pipeline()
                    pipeline.addSslSupport(request, ch)
                    pipeline.addLast(HttpClientCodec())
                    pipeline.addLast(HttpObjectAggregator(MAX_VALUE))

                    pipeline.addLast(object : SimpleChannelInboundHandler<FullHttpResponse>() {
                        override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpResponse) {
                            responsePromise.setSuccess(msg.toHttp4kResponse())
                            ctx.close()
                        }
                    })
                }
            })
            .connect(request.uri.host, request.portOrDefault())

        connectFuture.addListener { future ->
            if (!future.isSuccess) {
                val response = when (val cause = future.cause()) {
                    is ConnectException -> Response(CONNECTION_REFUSED.toClientStatus(cause))
                    is UnknownHostException -> Response(UNKNOWN_HOST.toClientStatus(cause))
                    is Exception -> Response(Status.INTERNAL_SERVER_ERROR.toClientStatus(cause))
                    else -> Response(Status.INTERNAL_SERVER_ERROR)
                }
                responsePromise.setSuccess(response)
            } else {
                connectFuture.channel().writeAndFlush(nettyRequest)
            }
        }

        return try {
            responsePromise.get(
                connectFuture.channel().config().connectTimeoutMillis.toLong(),
                TimeUnit.MILLISECONDS
            )
        } catch (e: Exception) {
            when (e) {
                is TimeoutException -> Response(Status.CLIENT_TIMEOUT)
                else -> Response(Status.INTERNAL_SERVER_ERROR.toClientStatus(e))
            }
        }
    }

    private fun FullHttpResponse.toHttp4kResponse() =
        Response(Status(status().code(), status().reasonPhrase()))
            .body(Body(content().nioBuffer())).let {
                headers().fold(it) { acc: Response, header: Map.Entry<String, String> ->
                    acc.header(header.key, header.value)
                }
            }

    private fun Request.portOrDefault(): Int = uri.port
        ?: when (uri.scheme) {
            "https" -> 443
            "http" -> 80
            else -> error("Unsupported scheme: ${uri.scheme}")
        }

    private fun ChannelPipeline.addSslSupport(request: Request, ch: SocketChannel) {
        if (request.uri.scheme == "https") {
            val sslContext: SslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build()
            addLast(sslContext.newHandler(ch.alloc(), request.uri.host, request.portOrDefault()))
        }
    }

    private fun Request.toNettyRequest(bodyMode: BodyMode): DefaultFullHttpRequest {
        val bodyBytes = body.payload.array()
        val nettyRequest = DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.valueOf(method.name),
            uri.toString(),

            Unpooled.wrappedBuffer(bodyBytes)
        )

        headers.forEach { (key, value) ->
            nettyRequest.headers().add(key, value)
        }

        if (bodyBytes.isNotEmpty()) {
            nettyRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, bodyBytes.size)
        }

        nettyRequest.headers().set(HttpHeaderNames.HOST, uri.host)
        nettyRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)

        return nettyRequest
    }

    companion object {
        private fun defaultBootstrapFactory(): BootstrapFactory = { eventLoopGroup ->
            Bootstrap().group(eventLoopGroup)
                .channel(NioSocketChannel::class.java)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
        }
    }
}
