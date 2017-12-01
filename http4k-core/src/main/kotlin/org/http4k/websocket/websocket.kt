package org.http4k.websocket

import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Status
import java.io.InputStream

interface Websocket {
    val upgradeRequest: Request
    fun send(message: WsMessage)
    fun close(status: Status)
    fun onError(fn: (Throwable) -> Unit)
    fun onClose(fn: (Status) -> Unit)
    fun onMessage(fn: (WsMessage) -> Unit)
}

typealias WsConsumer = (Websocket) -> Unit

typealias WsHandler = (Request) -> WsConsumer?

class PolyHandler(val http: HttpHandler, internal val ws: WsHandler)

data class WsMessage(val body: Body) {

    constructor(value: String) : this(Body(value))
    constructor(value: InputStream) : this(Body(value))

    fun body(new: Body): WsMessage = copy(body = new)
    fun bodyString(): String = String(body.payload.array())

    companion object
}
