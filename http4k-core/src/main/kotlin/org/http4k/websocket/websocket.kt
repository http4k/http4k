package org.http4k.websocket

import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.core.Status
import java.io.InputStream

interface WebSocket {
    val upgradeRequest: Request
    fun send(message: WsMessage)
    fun close(status: Status)
    fun onError(fn: (Throwable) -> Unit)
    fun onClose(fn: (Status) -> Unit)
    fun onMessage(fn: (WsMessage) -> Unit)
}

typealias WsConsumer = (WebSocket) -> Unit

typealias WsHandler = (Request) -> WsConsumer?

data class WsMessage(val body: Body) {

    constructor(value: String) : this(Body(value))
    constructor(value: InputStream) : this(Body(value))

    fun body(new: Body): WsMessage = copy(body = new)
    fun bodyString(): String = String(body.payload.array())

    companion object
}
