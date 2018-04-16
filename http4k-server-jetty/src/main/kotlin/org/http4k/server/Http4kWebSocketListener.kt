package org.http4k.server

import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketListener
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest
import org.http4k.core.Body
import org.http4k.core.Headers
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.StreamBody
import org.http4k.core.Uri
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus
import java.nio.ByteBuffer

internal class Http4kWebSocketAdapter internal constructor(private val innerSocket: PushPullAdaptingWebSocket) {
    fun onError(throwable: Throwable) = innerSocket.triggerError(throwable)
    fun onClose(statusCode: Int, reason: String?) = innerSocket.triggerClose(WsStatus(statusCode, reason ?: "<unknown>"))
    fun onMessage(body: Body) = innerSocket.triggerMessage(WsMessage(body))
}

internal fun ServletUpgradeRequest.asHttp4kRequest(): Request =
    Request(Method.valueOf(method), Uri.of(requestURI.toString()))
        .headers(headerParameters())

private fun ServletUpgradeRequest.headerParameters(): Headers = headers.asSequence().fold(listOf(), { memo, next -> memo + next.value.map { next.key to it } })

internal class Http4kWebSocketListener(private val wSocket: WsConsumer, private val upgradeRequest: Request) : WebSocketListener {
    private lateinit var websocket: Http4kWebSocketAdapter

    override fun onWebSocketClose(statusCode: Int, reason: String?) {
        websocket.onClose(statusCode, reason)
    }

    override fun onWebSocketConnect(session: Session) {
        websocket = Http4kWebSocketAdapter(object : PushPullAdaptingWebSocket(upgradeRequest) {
            override fun send(message: WsMessage) {
                when (message.body) {
                    is StreamBody -> session.remote.sendBytes(message.body.payload)
                    else -> session.remote.sendString(message.bodyString())
                }
            }

            override fun close(status: WsStatus) {
                session.close(status.code, status.description)
            }
        }.apply(wSocket))
    }

    override fun onWebSocketText(message: String) {
        websocket.onMessage(Body(message))
    }

    override fun onWebSocketBinary(payload: ByteArray, offset: Int, len: Int) {
        websocket.onMessage(Body(ByteBuffer.wrap(payload, offset, len)))
    }

    override fun onWebSocketError(cause: Throwable) {
        websocket.onError(cause)
    }
}