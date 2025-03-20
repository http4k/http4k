package org.http4k.bridge

import io.helidon.common.buffers.BufferData
import io.helidon.http.Headers
import io.helidon.http.HttpPrologue
import io.helidon.websocket.WsListener
import io.helidon.websocket.WsSession
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsMessage.Mode.Binary
import org.http4k.websocket.WsMessage.Mode.Text
import org.http4k.websocket.WsStatus
import java.util.*

class HelidonToHttp4kWebSocketListener(private val ws: WsHandler) : WsListener {

    private var complete = false
    private lateinit var upgradeRequest: Request
    private lateinit var websocket: PushPullAdaptingWebSocket

    override fun onMessage(session: WsSession, text: String, last: Boolean) {
        websocket.triggerMessage(WsMessage(text))
    }

    override fun onMessage(session: WsSession, buffer: BufferData, last: Boolean) {
        websocket.triggerMessage(WsMessage(buffer.readBytes()))
    }

    override fun onClose(session: WsSession, status: Int, reason: String) {
        websocket.triggerClose(WsStatus(status, reason))
    }

    override fun onError(session: WsSession, t: Throwable) {
        websocket.triggerError(t)
    }

    override fun onOpen(session: WsSession) {
        websocket = object : PushPullAdaptingWebSocket() {
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

    override fun onHttpUpgrade(prologue: HttpPrologue, headers: Headers): Optional<Headers> {
        assert(!complete) { "Cannot be reused for multiple sessions" }
        upgradeRequest = Request(GET, prologue.uriPath().rawPath() + prologue.query()).headers(headers)
        complete = true
        return super.onHttpUpgrade(prologue, headers)
    }
}
