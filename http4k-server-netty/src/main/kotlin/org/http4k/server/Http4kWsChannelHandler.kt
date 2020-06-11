package org.http4k.server

import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.StreamBody
import org.http4k.core.Uri
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus
import java.net.InetSocketAddress

class Http4kWsHandshakeListener(private val wsHandler: WsHandler) : ChannelInboundHandlerAdapter() {
    @Throws(Exception::class)
    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if(evt is WebSocketServerProtocolHandler.HandshakeComplete) {
            val address = ctx.channel().remoteAddress() as InetSocketAddress
            val upgradeRequest = evt.asHttp4kRequest(address)
            val wsConsumer = wsHandler(upgradeRequest)

            if(wsConsumer != null) {
                ctx.pipeline().addAfter(
                    ctx.name(),
                    Http4kWsChannelHandler::class.java.name,
                    Http4kWsChannelHandler(wsConsumer, upgradeRequest)
                )
            }
        }
    }
}

class Http4kWebSocketAdapter(private val innerSocket: PushPullAdaptingWebSocket) {
    fun onError(throwable: Throwable) = innerSocket.triggerError(throwable)
    fun onClose(status: WsStatus) = innerSocket.triggerClose(status)

    fun onMessage(body: Body) = innerSocket.triggerMessage(WsMessage(body))
}

class Http4kWsChannelHandler(private val wSocket: WsConsumer, private val upgradeRequest: Request): SimpleChannelInboundHandler<WebSocketFrame>() {
    private var websocket: Http4kWebSocketAdapter? = null
    private var normalClose = false;

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        println("A")
        websocket = Http4kWebSocketAdapter(object : PushPullAdaptingWebSocket(upgradeRequest) {
            override fun send(message: WsMessage) {
                when (message.body) {
                    is StreamBody -> ctx.writeAndFlush(BinaryWebSocketFrame(message.body.stream.use { Unpooled.wrappedBuffer(it.readBytes()) }))
                    else -> ctx.writeAndFlush(TextWebSocketFrame(message.bodyString()))
                }
            }

            override fun close(status: WsStatus) {
                ctx.writeAndFlush(CloseWebSocketFrame(status.code, status.description)).addListeners(ChannelFutureListener {
                    normalClose = true;
                    websocket?.onClose(status)
                }, ChannelFutureListener.CLOSE)
            }
        }.apply(wSocket))
    }

    override fun handlerRemoved(ctx: ChannelHandlerContext) {
        if(ctx.channel().isActive) {
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListeners(ChannelFutureListener {
                normalClose = true;
                websocket?.onClose(WsStatus.NOCODE)
            }, ChannelFutureListener.CLOSE)
        }
        websocket = null
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: WebSocketFrame) {
        when (msg) {
            is TextWebSocketFrame -> {
                websocket?.onMessage(Body(msg.text()))
            }
            is BinaryWebSocketFrame -> {
                websocket?.onMessage(Body(ByteBufInputStream(msg.content())))
            }
            is CloseWebSocketFrame -> {
                msg.retain()
                ctx.writeAndFlush(msg).addListeners(ChannelFutureListener {
                    websocket?.onClose(WsStatus(msg.statusCode(), msg.reasonText()))
                }, ChannelFutureListener.CLOSE)
            }
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        websocket?.onError(cause)
    }
}

internal fun WebSocketServerProtocolHandler.HandshakeComplete.asHttp4kRequest(address: InetSocketAddress) =
    Request(Method.GET, Uri.of(requestUri()))
        .headers(requestHeaders().map { it.key to it.value })
        .source(RequestSource(address.address.hostAddress, address.port))
