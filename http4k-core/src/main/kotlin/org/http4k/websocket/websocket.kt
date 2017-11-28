package org.http4k.websocket

import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.core.Status
import java.io.Closeable
import java.io.InputStream

interface WebSocket : Closeable {
    val upgradeRequest: Request
    fun send(message: WsMessage): WebSocket
    fun onError(fn: (Throwable) -> Unit): WebSocket
    fun onClose(fn: (Status) -> Unit): WebSocket
    fun onMessage(fn: (WsMessage) -> Unit): WebSocket
}

typealias WsConsumer = (WebSocket) -> Unit

interface WsHandler {
    operator fun invoke(request: Request): WsConsumer?
}

data class WsMessage(val body: Body) {

    constructor(value: String) : this(Body(value))
    constructor(value: InputStream) : this(Body(value))

    fun body(new: Body): WsMessage = copy(body = new)
    fun bodyString(): String = String(body.payload.array())

    companion object
}
