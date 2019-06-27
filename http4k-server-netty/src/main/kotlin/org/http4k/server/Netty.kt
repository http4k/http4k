package org.http4k.server


import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import io.netty.channel.ChannelFactory
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener.CLOSE
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.ServerChannel
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.DecoderResult.SUCCESS
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpUtil
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method.valueOf
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CONTINUE
import org.http4k.core.Uri
import org.http4k.core.safeLong
import org.http4k.core.then
import org.http4k.core.toParametersMap
import org.http4k.filter.ServerFilters
import java.net.InetSocketAddress


/**
 * Exposed to allow for insertion into a customised Netty server instance
 */
class Http4kChannelHandler(handler: HttpHandler) : SimpleChannelInboundHandler<FullHttpRequest>() {

    private val safeHandler = ServerFilters.CatchAll().then(handler)

    override fun channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest) {
        if (HttpUtil.is100ContinueExpected(request)) ctx.write(Response(CONTINUE).asNettyResponse())

        ctx.writeAndFlush(safeHandler(request.asRequest()).asNettyResponse()).apply {
            if (request.decoderResult() == SUCCESS) addListener(CLOSE) else ctx.close()
        }
    }

    private fun Response.asNettyResponse(): DefaultFullHttpResponse =
        DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus(status.code, status.description)).apply {
            headers.toParametersMap().forEach { (key, values) -> headers().set(key, values) }
            body.stream.use { it.copyTo(ByteBufOutputStream(content())) }
        }

    private fun FullHttpRequest.asRequest(): Request =
        Request(valueOf(method().name()), Uri.of(uri()))
            .headers(headers().map { it.key to it.value })
            .body(Body(ByteBufInputStream(content()), headers()["Content-Length"].safeLong()))
}

data class Netty(val port: Int = 8000) : ServerConfig {
    override fun toServer(httpHandler: HttpHandler): Http4kServer = object : Http4kServer {
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
                        ch.pipeline().addLast("aggregator", HttpObjectAggregator(Int.MAX_VALUE))
                        ch.pipeline().addLast("handler", Http4kChannelHandler(httpHandler))
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
            workerGroup.shutdownGracefully()
            masterGroup.shutdownGracefully()
        }

        override fun port(): Int = if (port > 0) 0 else address.port
    }
}