package org.http4k.server

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.HttpHeaderNames.CONNECTION
import io.netty.handler.codec.http.HttpHeaderValues.UPGRADE
import io.netty.handler.codec.http.HttpHeaderValues.WEBSOCKET
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.timeout.IdleStateHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Uri
import org.http4k.websocket.WsHandler
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.Executor

class WebSocketServerHandler(
    private val wsHandler: WsHandler,
    private val appExecutor: Executor,
    private val heartBeatInterval: Duration = Duration.ofSeconds(60)
) : ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HttpRequest) {
            val address = ctx.channel().remoteAddress() as InetSocketAddress
            val upgradeRequest = if (requiresWsUpgrade(msg)) msg.asWsUpgradeRequest(address) else null
            if (upgradeRequest != null) {
                val wsConsumer = wsHandler(upgradeRequest)

                val config = WebSocketServerProtocolConfig.newBuilder()
                    .handleCloseFrames(false)
                    .websocketPath(upgradeRequest.uri.toString())
                    .checkStartsWith(true)
                    .dropPongFrames(false)
                    .build()

                ctx.pipeline().addAfter(
                    ctx.name(),
                    "handshakeListener",
                    object : ChannelInboundHandlerAdapter() {
                        override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
                            if (evt is WebSocketServerProtocolHandler.HandshakeComplete) {
                                val heartBeatSeconds = heartBeatInterval.toSeconds().toInt()
                                ctx.pipeline().addAfter(
                                    ctx.name(),
                                    IdleStateHandler::class.java.name,
                                    IdleStateHandler(heartBeatSeconds * 3, heartBeatSeconds, 0)
                                )
                                ctx.pipeline().addAfter(
                                    IdleStateHandler::class.java.name,
                                    Http4kWsHeartbeatHandler::class.java.name,
                                    Http4kWsHeartbeatHandler()
                                )
                                ctx.pipeline().addAfter(
                                    Http4kWsHeartbeatHandler::class.java.name,
                                    Http4kWsChannelHandler::class.java.name,
                                    Http4kWsChannelHandler(wsConsumer, appExecutor)
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

                ctx.fireChannelRead(msg)
            } else {
                ctx.fireChannelRead(msg)
            }
        } else {
            ctx.fireChannelRead(msg)
        }
    }

    private fun requiresWsUpgrade(httpRequest: HttpRequest) =
        httpRequest.headers().containsValue(CONNECTION, UPGRADE, true) &&
            httpRequest.headers().containsValue(UPGRADE, WEBSOCKET, true)
}

internal fun HttpRequest.asWsUpgradeRequest(address: InetSocketAddress): Request? =
    Method.supportedOrNull(method().name())?.let { method ->
        Request(method, Uri.of(uri()))
            .headers(headers().map { it.key to it.value })
            .source(RequestSource(address.address.hostAddress, address.port))
    }
