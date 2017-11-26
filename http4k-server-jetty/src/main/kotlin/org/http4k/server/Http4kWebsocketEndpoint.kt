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
import java.io.Closeable
import java.nio.ByteBuffer

interface WsSession : Closeable, (Body) -> Unit

interface WSocket {
    fun onError(throwable: Throwable, session: WsSession)
    fun onClose(status: Status, session: WsSession)
    fun onMessage(body: Body, session: WsSession)
}

class JettyWsSession(private val session: Session, private val wSocket: WSocket) {
    private val sender = object : WsSession {
        override fun invoke(p1: Body) {
            when (p1) {
                is StreamBody -> session.remote.sendBytes(p1.payload)
                else -> session.remote.sendString(p1.toString())
            }
        }

        override fun close() {
            session.close()
        }
    }

    fun onError(throwable: Throwable) = wSocket.onError(throwable, sender)
    fun onClose(statusCode: Int, reason: String?) = wSocket.onClose(Status(statusCode, reason ?: ""), sender)
    fun onMessage(body: Body) = wSocket.onMessage(body, sender)
}

interface WebsocketRouter {
    fun match(request: Request): WSocket?
}

internal fun ServletUpgradeRequest.asHttp4kRequest(): Request =
    Request(Method.valueOf(method), Uri.of(requestURI.toString() + queryString.toQueryString()))
        .headers(headerParameters())

private fun ServletUpgradeRequest.headerParameters(): Headers = headers.asSequence().fold(listOf(), { memo, next -> memo + next.value.map { next.key to it } })

private fun String?.toQueryString(): String = if (this != null && this.isNotEmpty()) "?" + this else ""

class Http4kWebsocketEndpoint(private val wSocket: WSocket) : WebSocketListener {
    private var websocket: JettyWsSession? = null

    override fun onWebSocketClose(statusCode: Int, reason: String?) {
        websocket?.onClose(statusCode, reason)
    }

    override fun onWebSocketConnect(session: Session) {
        websocket = JettyWsSession(session, wSocket)
    }

    override fun onWebSocketText(message: String) {
        websocket?.onMessage(Body(message))
    }

    override fun onWebSocketBinary(payload: ByteArray, offset: Int, len: Int) {
        websocket?.onMessage(Body(ByteBuffer.wrap(payload, offset, len)))
    }

    override fun onWebSocketError(cause: Throwable) {
        websocket?.onError(cause)
    }
}