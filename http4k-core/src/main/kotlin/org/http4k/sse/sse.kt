package org.http4k.sse

import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.websocket.Websocket
import java.io.InputStream

interface Sse {
    val connectRequest: Request
    fun send(message: SseMessage)
    fun close(status: SseStatus = SseStatus.NORMAL)
    fun onError(fn: (Throwable) -> Unit)
    fun onClose(fn: (SseStatus) -> Unit)
}

typealias SseConsumer = (Websocket) -> Unit

typealias SseHandler = (Request) -> SseConsumer?

/**
 * A PolyHandler represents the combined routing logic of an HTTP handler and an SSE handler.
 */
class PolyHandler(val http: HttpHandler, internal val sse: SseHandler)

data class SseMessage(val body: Body) {
    constructor(value: String) : this(Body(value))
    constructor(value: InputStream) : this(Body(value))

    fun body(new: Body): SseMessage = copy(body = new)
    fun bodyString(): String = String(body.payload.array())

    companion object
}
