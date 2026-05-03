package org.http4k.testing

import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with
import org.http4k.lens.Header
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.concurrent.thread

/**
 * This class is a test client for a [PolyHandler] which allows the invocation of the underlying handlers.
 */
class PolyHandlerTestClient(private val poly: PolyHandler) {

    /**
     * Invoke the HTTP handler of the [PolyHandler] with the given [Request].
     */
    fun http(request: Request) = poly.http?.invoke(request) ?: error("http not implemented")

    /**
     * Invoke the WebSocket handler of the [PolyHandler] with the given [Request].
     */
    fun ws(request: Request) = poly.ws?.testWsClient(request) ?: error("ws not implemented")

    /**
     * Invoke the Server-Sent Events handler of the [PolyHandler] with the given [Request].
     */
    fun sse(request: Request) = poly.sse?.testSseClient(request) ?: error("sse not implemented")
}

/**
 * Converts a [PolyHandler] into an [HttpHandler] for testing, dispatching SSE requests
 * to the SSE handler (via piped streams) and all other requests to the HTTP handler.
 */
fun PolyHandler.toHttpHandler(): HttpHandler {
    val polyClient = PolyHandlerTestClient(this)
    return { request ->
        if (request.header("Accept")?.contains("text/event-stream") == true) {
            val sseClient = polyClient.sse(request)
            val pipedIn = PipedInputStream()
            val pipedOut = PipedOutputStream(pipedIn)
            thread(isDaemon = true) {
                try {
                    sseClient.received().forEach { pipedOut.write(it.toMessage().toByteArray()) }
                } catch (_: Exception) {
                } finally {
                    pipedOut.close()
                }
            }
            Response(sseClient.status)
                .with(Header.CONTENT_TYPE of ContentType.TEXT_EVENT_STREAM)
                .body(pipedIn)
        } else {
            polyClient.http(request)
        }
    }
}
