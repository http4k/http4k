package org.http4k.server

import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderNames.CONNECTION
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpHeaderValues.*
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import io.netty.handler.codec.http.LastHttpContent
import io.netty.handler.codec.http.LastHttpContent.*
import io.netty.handler.stream.ChunkedStream
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.MemoryBody
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status
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
        val address = ctx.channel().remoteAddress() as? InetSocketAddress
        val response = request.asRequest(address)?.let(safeHandler) ?: Response(Status.NOT_IMPLEMENTED)

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
                            headers().set(CONNECTION, KEEP_ALIVE)
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
                        headers().set(CONNECTION, KEEP_ALIVE)
                    }
                ctx.write(httpResponse)
                ctx.write(ChunkedStream(response.body.stream))
                ctx.writeAndFlush(EMPTY_LAST_CONTENT)
            }
        }
    }

    private fun FullHttpRequest.asRequest(address: InetSocketAddress?) = runCatching {
        val baseRequest = Request(Method.valueOf(method().name()), Uri.of(uri()))
            .headers(headers().map { it.key to it.value })
            .body(Body(ByteBufInputStream(content()), headers()["Content-Length"].safeLong()))
        address?.let { baseRequest.source(RequestSource(it.address.hostAddress, it.port)) } ?: baseRequest
    }.getOrNull()
}
