package org.http4k.server

import io.helidon.common.buffers.BufferData
import io.helidon.common.uri.UriPath
import io.helidon.http.HttpPrologue
import io.helidon.webserver.websocket.WsConnection
import io.helidon.websocket.WsListener
import io.helidon.websocket.WsSession
import org.http4k.core.Method.GET
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsMessage.Mode.Binary
import org.http4k.websocket.WsMessage.Mode.Text
import org.http4k.websocket.WsStatus

class HelidonWebSockerListener(private val ws: WsHandler) : WsListener {

    private val sessions = mutableMapOf<WsSession, PushPullAdaptingWebSocket>()

    override fun onMessage(session: WsSession, text: String, last: Boolean) {
        wsFor(session).triggerMessage(WsMessage(text))
    }

    override fun onMessage(session: WsSession, buffer: BufferData, last: Boolean) {
        wsFor(session).triggerMessage(WsMessage(buffer.readBytes()))
    }

    override fun onClose(session: WsSession, status: Int, reason: String) {
        wsFor(session).triggerClose(WsStatus(status, reason))
        sessions.remove(session)
    }

    override fun onError(session: WsSession, t: Throwable) {
        wsFor(session).triggerError(t)
    }

    override fun onOpen(session: WsSession) {
        wsFor(session)
    }

    private fun wsFor(session: WsSession): PushPullAdaptingWebSocket {
        return sessions.getOrPut(session) {
            val uriPath = uriPath.get(prologue.get(session) as HttpPrologue) as UriPath
            val query = rawQuery.get(prologue.get(session) as HttpPrologue)
            val upgradeRequest = org.http4k.core.Request(GET, uriPath.rawPath() + "?" + query)
            object : PushPullAdaptingWebSocket() {
                override fun send(message: WsMessage) {
                    when (message.mode) {
                        Binary -> session.send(BufferData.create(message.body.payload.array()), true)
                        Text -> session.send(message.bodyString(), true)
                    }
                }

                override fun close(status: WsStatus) {
                    session.close(status.code, status.description)
                }
            }.apply(ws(upgradeRequest))
        }
    }

    private val prologue = WsConnection::class.java.getDeclaredField("prologue").also {
        it.isAccessible = true
    }
    private val uriPath = HttpPrologue::class.java.getDeclaredField("uriPath").also {
        it.isAccessible = true
    }
    private val rawQuery = HttpPrologue::class.java.getDeclaredField("rawQuery").also {
        it.isAccessible = true
    }
}
