package org.http4k.server


import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
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
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method.valueOf
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters


/**
 * Exposed to allow for insertion into a customised Netty server instance
 */
class Http4kChannelHandler(handler: HttpHandler) : SimpleChannelInboundHandler<FullHttpRequest>() {

    private val safeHandler = ServerFilters.CatchAll().then(handler)

    override fun channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest): Unit {
        try {
            if (request.decoderResult() == SUCCESS) {
                ctx.writeAndFlush(safeHandler(request.asRequest()).asNettyResponse())
            }
        } finally {
            ctx.close()
        }
    }

    private fun Response.asNettyResponse(): DefaultFullHttpResponse {
        val nettyBody = Unpooled.buffer()
        val out = ByteBufOutputStream(nettyBody)
        val res = DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus(status.code, status.description), nettyBody)
        headers.forEach { (key, value) -> res.headers().set(key, value) }
        body.stream.use{ input -> out.use { output -> input.copyTo(output) }}
        return res
    }

    private fun FullHttpRequest.asRequest(): Request =
        headers().fold(Request(valueOf(method().name()), Uri.Companion.of(uri()))) {
            memo, next ->
            memo.header(next.key, next.value)
        }.body(Body(ByteBufInputStream(content())))
}

data class Netty(val port: Int = 8000) : ServerConfig {
    override fun toServer(handler: HttpHandler): Http4kServer {
        return object : Http4kServer {
            private val masterGroup = NioEventLoopGroup()
            private val workerGroup = NioEventLoopGroup()
            private var closeFuture: ChannelFuture? = null

            override fun start(): Http4kServer {
                val bootstrap = ServerBootstrap()
                bootstrap.group(masterGroup, workerGroup)
                    .channel(NioServerSocketChannel::class.java)
                    .childHandler(object : ChannelInitializer<SocketChannel>() {
                        public override fun initChannel(ch: SocketChannel) {
                            ch.pipeline().addLast("codec", HttpServerCodec())
                            ch.pipeline().addLast("aggregator", HttpObjectAggregator(Int.MAX_VALUE))
                            ch.pipeline().addLast("handler", Http4kChannelHandler(handler))
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)

                closeFuture = bootstrap.bind(port).sync().channel().closeFuture()
                return this
            }

            override fun stop() {
                closeFuture?.cancel(false)
                workerGroup.shutdownGracefully()
                masterGroup.shutdownGracefully()
            }
        }
    }
}
