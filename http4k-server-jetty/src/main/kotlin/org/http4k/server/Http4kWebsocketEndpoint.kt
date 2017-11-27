package org.http4k.server

import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketListener
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest
import org.http4k.core.Body
import org.http4k.core.Headers
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.StreamBody
import org.http4k.core.Uri
import org.http4k.websocket.WebSocket
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage
import java.nio.ByteBuffer


internal class MutableInboundWebSocket(private val session: Session) : WebSocket {

    var errorHandlers: MutableList<(Throwable) -> Unit> = mutableListOf()
    var closeHandlers: MutableList<(Status) -> Unit> = mutableListOf()
    var messageHandlers: MutableList<(WsMessage) -> Unit> = mutableListOf()

    override fun invoke(message: WsMessage) = when (message.body) {
        is StreamBody -> session.remote.sendBytes(message.body.payload)
        else -> session.remote.sendString(message.toString())
    }

    fun triggerError(throwable: Throwable) = errorHandlers.forEach { it(throwable) }
    fun triggerClose(status: Status) = closeHandlers.forEach { it(status) }
    fun triggerMessage(message: WsMessage) = messageHandlers.forEach { it(message) }

    override fun onError(fn: (Throwable) -> Unit) {
        errorHandlers.add(fn)
    }

    override fun onClose(fn: (Status) -> Unit) {
        closeHandlers.add(fn)
    }

    override fun onMessage(fn: (WsMessage) -> Unit) {
        messageHandlers.add(fn)
    }

    override fun close() {
        session.close()
    }
}

internal class Http4kWebSocketAdapter internal constructor(private val innerSocket: MutableInboundWebSocket) : Http4kWebSocket {
    override fun invoke(p1: WsMessage) {
        innerSocket(p1)
    }

    override fun close() {
        innerSocket.close()
    }

    fun onError(throwable: Throwable) = innerSocket.triggerError(throwable)
    fun onClose(statusCode: Int, reason: String?) = innerSocket.triggerClose(Status(statusCode, reason ?: "<unknown>"))
    fun onMessage(body: Body) = innerSocket.triggerMessage(WsMessage(body))
}

internal fun ServletUpgradeRequest.asHttp4kRequest(): Request =
    Request(Method.valueOf(method), Uri.of(requestURI.toString() + queryString.toQueryString()))
        .headers(headerParameters())

private fun ServletUpgradeRequest.headerParameters(): Headers = headers.asSequence().fold(listOf(), { memo, next -> memo + next.value.map { next.key to it } })

private fun String?.toQueryString(): String = if (this != null && this.isNotEmpty()) "?" + this else ""

internal class Http4kWebsocketEndpoint(private val wSocket: WsHandler) : WebSocketListener {
    private lateinit var websocket: Http4kWebSocketAdapter

    override fun onWebSocketClose(statusCode: Int, reason: String?) {
        websocket.onClose(statusCode, reason)
    }

    override fun onWebSocketConnect(session: Session) {
        websocket = Http4kWebSocketAdapter(MutableInboundWebSocket(session).apply(wSocket))
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