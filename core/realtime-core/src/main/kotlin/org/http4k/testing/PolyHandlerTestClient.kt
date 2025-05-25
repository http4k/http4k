package org.http4k.testing

import org.http4k.core.PolyHandler
import org.http4k.core.Request

/**
 * This class is a test client for a [PolyHandler] which allows the invocation of the underlying handlers.
 */
class PolyHandlerTestClient(private val poly: PolyHandler) {

    /**
     * Invoke the HTTP handler of the [PolyHandler] with the given [Request].
     */
    suspend fun http(request: Request) = poly.http?.invoke(request) ?: error("http not implemented")

    /**
     * Invoke the WebSocket handler of the [PolyHandler] with the given [Request].
     */
    suspend fun ws(request: Request) = poly.ws?.testWsClient(request) ?: error("ws not implemented")

    /**
     * Invoke the Server-Sent Events handler of the [PolyHandler] with the given [Request].
     */
    suspend fun sse(request: Request) = poly.sse?.testSseClient(request) ?: error("sse not implemented")
}
