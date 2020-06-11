package org.http4k.server

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler

class WebSocketServerHandler : ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HttpRequest) {
            if (requiresUpgrade(msg)) {
                val config= WebSocketServerProtocolConfig.newBuilder()
                    .handleCloseFrames(false)
                    .websocketPath("/")
                    .checkStartsWith(true)
                    .build()

                ctx.pipeline().addAfter(
                    ctx.name(),
                    WebSocketServerProtocolHandler::class.java.name,
                    WebSocketServerProtocolHandler(config)
                )
                ctx.fireChannelRead(msg)
            } else {
                ctx.fireChannelRead(msg)
            }
        } else {
            ctx.fireChannelRead(msg)
        }
    }

    private fun requiresUpgrade(httpRequest: HttpRequest): Boolean {
        val headers = httpRequest.headers()
        return headers.containsValue(
            HttpHeaderNames.CONNECTION,
            HttpHeaderValues.UPGRADE,
            true
        ) && headers
            .containsValue(
                HttpHeaderNames.UPGRADE,
                HttpHeaderValues.WEBSOCKET,
                true
            )
    }
}
