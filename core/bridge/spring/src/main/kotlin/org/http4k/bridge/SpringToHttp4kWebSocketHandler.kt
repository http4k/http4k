package org.http4k.bridge

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsMessage.Mode.Binary
import org.http4k.websocket.WsMessage.Mode.Text
import org.http4k.websocket.WsStatus
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

class SpringToHttp4kWebSocketHandler(private val ws: WsHandler) : WebSocketHandler {

    private val sockets = ConcurrentHashMap<String, PushPullAdaptingWebSocket>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val upgradeRequest = session.toHttp4kRequest()
        val adapter = object : PushPullAdaptingWebSocket() {
            override fun send(message: WsMessage) {
                when (message.mode) {
                    Text -> session.sendMessage(TextMessage(message.bodyString()))
                    Binary -> session.sendMessage(BinaryMessage(message.body.payload))
                }
            }

            override fun close(status: WsStatus) {
                session.close(CloseStatus(status.code, status.description))
            }
        }
        sockets[session.id] = adapter
        ws(upgradeRequest).consumer(adapter)
    }

    override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
        val socket = sockets[session.id] ?: return
        when (message) {
            is TextMessage -> socket.triggerMessage(WsMessage(message.payload))
            is BinaryMessage -> {
                val buf = message.payload
                val bytes = ByteArray(buf.remaining()).also { buf.duplicate().get(it) }
                socket.triggerMessage(WsMessage(bytes))
            }
        }
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        sockets[session.id]?.triggerError(exception)
    }

    override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
        sockets.remove(session.id)?.triggerClose(WsStatus(closeStatus.code, closeStatus.reason ?: ""))
    }

    override fun supportsPartialMessages() = false
}

private fun WebSocketSession.toHttp4kRequest(): Request {
    val u = uri ?: java.net.URI("/")
    val target = Uri.of(u.rawPath + (u.rawQuery?.let { "?$it" } ?: ""))
    val headers = handshakeHeaders.headerSet().flatMap { (name, values) -> values.map { name to it } }
    return Request(GET, target).headers(headers)
}
