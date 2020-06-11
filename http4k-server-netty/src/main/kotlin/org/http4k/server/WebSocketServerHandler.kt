package org.http4k.server

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Uri
import org.http4k.websocket.WsHandler
import java.net.InetSocketAddress

class WebSocketServerHandler(private val wsHandler: WsHandler) : ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HttpRequest) {
            if (requiresUpgrade(msg)) {
                val address = ctx.channel().remoteAddress() as InetSocketAddress
                val upgradeRequest = msg.asRequest(address)
                val wsConsumer = wsHandler(upgradeRequest)

                if(wsConsumer != null) {
                    val config= WebSocketServerProtocolConfig.newBuilder()
                        .handleCloseFrames(false)
                        .websocketPath("/")
                        .checkStartsWith(true)
                        .build()

                    ctx.pipeline().addAfter(
                        ctx.name(),
                        "handshakeListener",
                        object: ChannelInboundHandlerAdapter() {
                            override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
                                if(evt is WebSocketServerProtocolHandler.HandshakeComplete) {
                                    ctx.pipeline().addAfter(
                                        ctx.name(),
                                        Http4kWsChannelHandler::class.java.name,
                                        Http4kWsChannelHandler(wsConsumer, upgradeRequest)
                                    )
                                }
                            }
                        }
                    )

                    ctx.pipeline().addAfter(
                        ctx.name(),
                        WebSocketServerProtocolHandler::class.java.name,
                        WebSocketServerProtocolHandler(config)
                    )
                } else {
                    ctx.write(DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SERVICE_UNAVAILABLE))
                }

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

    private fun HttpRequest.asRequest(address: InetSocketAddress) =
        Request(Method.valueOf(method().name()), Uri.of(uri()))
            .headers(headers().map { it.key to it.value })
            .source(RequestSource(address.address.hostAddress, address.port))
}
