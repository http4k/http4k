package org.http4k.server

import org.eclipse.jetty.websocket.api.Callback
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.exceptions.WebSocketException
import org.http4k.core.Request
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus
import java.nio.ByteBuffer

class Http4kJettyServerWebSocketEndpoint(
    private val consumer: WsConsumer, private val request: Request
) : Session.Listener.AbstractAutoDemanding() {
    private var websocket: PushPullAdaptingWebSocket? = null

    override fun onWebSocketOpen(session: Session) {
        super.onWebSocketOpen(session)
        websocket = object : PushPullAdaptingWebSocket() {
            override fun send(message: WsMessage) {
                if (!isOpen) {
                    throw WebSocketException("Connection to ${request.uri} is closed.")
                }
                try {
                    when (message.mode) {
                        WsMessage.Mode.Binary -> Callback.Completable.with { session.sendBinary(message.body.payload, it) }.get()
                        WsMessage.Mode.Text -> Callback.Completable.with { session.sendText(message.body.toString(), it) }.get()
                    }
                } catch (error: Throwable) {
                    triggerError(error)
                }
            }

            override fun close(status: WsStatus) {
                if (isOpen) {
                    Callback.Completable.with { session.close(status.code, status.description, it) }.get()
                }
            }

        }.apply(consumer)
    }

    override fun onWebSocketClose(statusCode: Int, reason: String?) {
        super.onWebSocketClose(statusCode, reason)
        websocket?.triggerClose(WsStatus(statusCode, reason.orEmpty()))
    }

    override fun onWebSocketError(cause: Throwable) {
        super.onWebSocketError(cause)
        websocket?.triggerError(cause)
    }

    override fun onWebSocketText(message: String) {
        super.onWebSocketText(message)
        try {
            websocket?.triggerMessage(WsMessage(message))
        } catch (e: Throwable) {
            websocket?.triggerError(e)
        }
    }

    override fun onWebSocketBinary(payload: ByteBuffer, callback: Callback) {
        super.onWebSocketBinary(payload, callback)
        try {
            websocket?.triggerMessage(WsMessage(payload))
            callback.succeed()
        } catch (e: Throwable) {
            websocket?.triggerError(e)
            callback.fail(e)
        }
    }
}
