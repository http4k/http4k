package org.http4k.server

import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent

/**
 * Configure the server-initiated heartbeat.
 * Send a PING when the server writer is idle.
 * Close the connection if the server reader is idle (i.e. the client never responsed with a PONG)
 */
class Http4kWsHeartbeatHandler: ChannelInboundHandlerAdapter() {
    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is IdleStateEvent) {
            when(evt.state()) {
                IdleState.WRITER_IDLE -> ctx
                    .writeAndFlush(PingWebSocketFrame())
                IdleState.READER_IDLE -> ctx
                    .writeAndFlush(CloseWebSocketFrame(1000, "Idle Timeout"))
                    .addListener(ChannelFutureListener.CLOSE)
                IdleState.ALL_IDLE -> {}
            }
        }
        ctx.fireUserEventTriggered(evt)
    }
}
