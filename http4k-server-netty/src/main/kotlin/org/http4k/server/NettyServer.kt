package org.http4k.server


import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled.wrappedBuffer
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.DefaultHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames.CONNECTION
import io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH
import io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE
import io.netty.handler.codec.http.HttpResponseStatus.CONTINUE
import io.netty.handler.codec.http.HttpResponseStatus.OK
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpUtil.is100ContinueExpected
import io.netty.handler.codec.http.HttpUtil.isKeepAlive
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.http4k.http.core.HttpHandler
import org.http4k.http.core.Method
import org.http4k.http.core.Request
import org.http4k.http.core.Response
import org.http4k.http.core.Uri
import java.nio.ByteBuffer

private class RequestHandler(private val handler: HttpHandler) : ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext, request: Any) {
        if (request is DefaultHttpRequest) {
            if (is100ContinueExpected(request)) {
                ctx.write(DefaultFullHttpResponse(HTTP_1_1, CONTINUE))
            }

            val res = handler(request.asRequest()).asNettyResponse()

            if (isKeepAlive(request)) {
                res.headers().set(CONNECTION, KEEP_ALIVE)
                ctx.write(res)
            } else {
                ctx.write(res).addListener(ChannelFutureListener.CLOSE)
            }
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ctx.close()
    }
}

private fun Response.asNettyResponse(): DefaultFullHttpResponse {
    val res = DefaultFullHttpResponse(HTTP_1_1, OK,
        this.body?.let { wrappedBuffer(it) } ?: wrappedBuffer("".toByteArray())
    )
    headers.forEach { (key, value) -> res.headers().set(key, value) }
    res.headers().set(CONTENT_LENGTH, res.content().readableBytes())
    return res
}

private fun DefaultHttpRequest.asRequest(): Request =
    // FIXME - if the method is unknown
    Request(Method.valueOf(method().name()), Uri.Companion.uri(uri()),
        headers().map { entry -> entry.key to entry.value },
        when (this) {
            is DefaultFullHttpRequest -> ByteBuffer.wrap(this.content().array())
            else -> null
        })

class NettyServer(private val port: Int, private val handler: HttpHandler) {
    private val masterGroup = NioEventLoopGroup()
    private val workerGroup = NioEventLoopGroup()
    private var closeFuture: ChannelFuture? = null

    fun start(): NettyServer {
        val bootstrap = ServerBootstrap()
        bootstrap.group(masterGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                public override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast("codec", HttpServerCodec())
                    ch.pipeline().addLast("handler", RequestHandler(handler))
                }
            })
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_KEEPALIVE, true)

        closeFuture = bootstrap.bind(port).sync().channel().closeFuture()
        return this
    }

    fun block() {
        closeFuture?.sync()
    }

    fun stop() {
        // FIXME is this correct??!
        closeFuture?.cancel(false)
        workerGroup.shutdownGracefully()
        masterGroup.shutdownGracefully()
    }
}

fun HttpHandler.asNettyServer(port: Int) = NettyServer(port, this)