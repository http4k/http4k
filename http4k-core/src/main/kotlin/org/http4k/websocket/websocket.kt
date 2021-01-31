package org.http4k.websocket

import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.websocket.WsStatus.Companion.NORMAL
import java.io.InputStream

/**
 * Represents a connected Websocket instance, and can be passed around an application. This is configured
 * to react to events on the WS event stream by attaching listeners.
 */
interface Websocket {
    val upgradeRequest: Request
    fun send(message: WsMessage)
    fun close(status: WsStatus = NORMAL)
    fun onError(fn: (Throwable) -> Unit)
    fun onClose(fn: (WsStatus) -> Unit)
    fun onMessage(fn: (WsMessage) -> Unit)
}

typealias WsConsumer = (Websocket) -> Unit

typealias WsHandler = (Request) -> WsConsumer?

@Deprecated("moved to org.http4k.server", ReplaceWith("org.http4k.server.PolyHandler"))
typealias PolyHandler = org.http4k.server.PolyHandler

data class WsMessage(val body: Body) {
    constructor(value: String) : this(Body(value))
    constructor(value: InputStream) : this(Body(value))

    fun body(new: Body): WsMessage = copy(body = new)
    fun bodyString(): String = String(body.payload.array())

    companion object
}
