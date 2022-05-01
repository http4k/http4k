package org.http4k.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFactory
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.ServerChannel
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpServerKeepAliveHandler
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http.LastHttpContent
import io.netty.handler.stream.ChunkedStream
import io.netty.handler.stream.ChunkedWriteHandler
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.MemoryBody
import org.http4k.core.Method.valueOf
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Uri
import org.http4k.core.safeLong
import org.http4k.core.then
import org.http4k.core.toParametersMap
import org.http4k.filter.ServerFilters
import org.http4k.server.ServerConfig.StopMode
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsHandler
import java.net.InetSocketAddress
import java.time.Duration.ofSeconds
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * Exposed to allow for insertion into a customised Netty server instance
 */
class Http4kChannelHandler(handler: HttpHandler) : SimpleChannelInboundHandler<FullHttpRequest>() {
    private val safeHandler = ServerFilters.CatchAll().then(handler)

    override fun channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest) {
        val address = ctx.channel().remoteAddress() as? InetSocketAddress
        val response = safeHandler(request.asRequest(address))

        when (response.body) {
            is MemoryBody -> {
                val byteBuf = Unpooled.wrappedBuffer(response.body.payload)
                val httpResponse =
                    DefaultFullHttpResponse(
                        HTTP_1_1,
                        HttpResponseStatus(response.status.code, response.status.description),
                        byteBuf
                    )
                        .apply {
                            response.headers.toParametersMap().forEach { (key, values) -> headers().set(key, values) }
                            headers().set(HttpHeaderNames.CONTENT_LENGTH, byteBuf.readableBytes())
                            headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                        }
                ctx.writeAndFlush(httpResponse)
            }
            else -> {
                val httpResponse =
                    DefaultHttpResponse(
                        HTTP_1_1,
                        HttpResponseStatus(response.status.code, response.status.description)
                    ).apply {
                        response.headers.toParametersMap().forEach { (key, values) -> headers().set(key, values) }
                        headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                    }
                ctx.write(httpResponse)
                ctx.write(ChunkedStream(response.body.stream))
                ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
            }
        }
    }

    private fun FullHttpRequest.asRequest(address: InetSocketAddress?): Request {
        val baseRequest = Request(valueOf(method().name()), Uri.of(uri()))
            .headers(headers().map { it.key to it.value })
            .body(Body(ByteBufInputStream(content()), headers()["Content-Length"].safeLong()))
        return address?.let { baseRequest.source(RequestSource(it.address.hostAddress, it.port)) } ?: baseRequest
    }
}

class Netty(val port: Int = 8000, override val stopMode: StopMode) : PolyServerConfig {
    constructor(port: Int = 8000) : this(port, StopMode.Graceful(ofSeconds(15)))

    val shutdownTimeoutMillis = when(stopMode) {
        is StopMode.Graceful -> stopMode.timeout.toMillis()
        is StopMode.Immediate -> throw ServerConfig.UnsupportedStopMode(stopMode)
    }

    override fun toServer(http: HttpHandler?, ws: WsHandler?, sse: SseHandler?): Http4kServer = object : Http4kServer {
        init {
            if (sse != null) throw UnsupportedOperationException("Netty does not support sse")
        }

        private val masterGroup = NioEventLoopGroup()
        private val workerGroup = NioEventLoopGroup()
        private var closeFuture: ChannelFuture? = null
        private lateinit var address: InetSocketAddress

        override fun start(): Http4kServer = apply {
            val bootstrap = ServerBootstrap()
            bootstrap.group(masterGroup, workerGroup)
                .channelFactory(ChannelFactory<ServerChannel> { NioServerSocketChannel() })
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    public override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast("codec", HttpServerCodec())
                        ch.pipeline().addLast("keepAlive", HttpServerKeepAliveHandler())
                        ch.pipeline().addLast("aggregator", HttpObjectAggregator(Int.MAX_VALUE))

                        if (ws != null) ch.pipeline().addLast("websocket", WebSocketServerHandler(ws))

                        ch.pipeline().addLast("streamer", ChunkedWriteHandler())
                        if (http != null) ch.pipeline().addLast("httpHandler", Http4kChannelHandler(http))
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 1000)
                .childOption(ChannelOption.SO_KEEPALIVE, true)

            val channel = bootstrap.bind(port).sync().channel()
            address = channel.localAddress() as InetSocketAddress
            closeFuture = channel.closeFuture()
        }

        override fun stop() = apply {
            closeFuture?.cancel(false)

            val sleepTime = minOf(2000L, shutdownTimeoutMillis)
            workerGroup.shutdownGracefully(sleepTime, shutdownTimeoutMillis, MILLISECONDS).sync()
            masterGroup.shutdownGracefully(sleepTime, shutdownTimeoutMillis, MILLISECONDS).sync()
        }

        override fun port(): Int = if (port > 0) port else address.port
    }
}
