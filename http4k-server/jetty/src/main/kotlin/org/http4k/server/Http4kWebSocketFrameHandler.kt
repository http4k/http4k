package org.http4k.server

import org.eclipse.jetty.util.Callback
import org.eclipse.jetty.websocket.core.CloseStatus
import org.eclipse.jetty.websocket.core.CoreSession
import org.eclipse.jetty.websocket.core.Frame
import org.eclipse.jetty.websocket.core.FrameHandler
import org.eclipse.jetty.websocket.core.OpCode.BINARY
import org.eclipse.jetty.websocket.core.OpCode.TEXT
import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.core.StreamBody
import org.http4k.websocket.Http4kWebSocketAdapter
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus

class Http4kWebSocketFrameHandler(private val wSocket: WsConsumer,
                                  private val upgradeRequest: Request) : FrameHandler {

    private var websocket: Http4kWebSocketAdapter? = null

    override fun onFrame(frame: Frame, callback: Callback) {
        when (frame.opCode) {
            TEXT, BINARY -> websocket?.onMessage(Body(frame.payloadAsUTF8))
        }
        callback.succeeded()
    }

    override fun onOpen(session: CoreSession, callback: Callback) {
        websocket = Http4kWebSocketAdapter(object : PushPullAdaptingWebSocket(upgradeRequest) {
            override fun send(message: WsMessage) {
                session.sendFrame(Frame(
                    if (message.body is StreamBody) BINARY else TEXT,
                    message.body.payload), object : Callback {
                    override fun succeeded() = session.flush(object : Callback {})
                }, false)
            }

            override fun close(status: WsStatus) {
                session.close(status.code, status.description, object : Callback {
                    override fun succeeded() = session.flush(object : Callback {})
                })
            }
        }.apply(wSocket))
        callback.succeeded()
    }

    override fun onError(cause: Throwable, callback: Callback) {
        websocket?.onError(cause)
        callback.succeeded()
    }

    override fun onClosed(closeStatus: CloseStatus, callback: Callback) {
        websocket?.onClose(WsStatus(closeStatus.code, closeStatus.reason ?: "<unknown>"))
        callback.succeeded()
    }
}
