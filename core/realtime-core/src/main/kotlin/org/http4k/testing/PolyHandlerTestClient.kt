package org.http4k.testing

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.Header
import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.ByteBuffer
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
 * Converts a [PolyHandler] into an [HttpHandler], dispatching SSE requests to the SSE handler
 * (forwarded as a true byte stream via piped streams so persistent streams do not block) and all
 * other requests to the HTTP handler.
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
                    pipedOut.use { out ->
                        sseClient.received().forEach { msg ->
                            out.write(msg.toMessage().toByteArray(Charsets.UTF_8))
                            out.flush()
                        }
                    }
                } catch (_: IOException) {
                    sseClient.close()
                }
            }

            sseClient.response
                .with(Header.CONTENT_TYPE of ContentType.TEXT_EVENT_STREAM)
                .body(CapturingBody(pipedIn))
        } else {
            polyClient.http(request)
        }
    }
}

/**
 * Body that tees bytes from [source] into an internal buffer as the consumer reads, so downstream
 * recorders that access [payload] / [text] after consumption see the captured bytes rather than an
 * exhausted stream. Subsequent accesses of [stream] after EOF replay from the captured buffer.
 */
private class CapturingBody(source: InputStream, override val length: Long? = null) : Body {
    private val capture = ByteArrayOutputStream()
    private var consumed = false

    private val tee = object : FilterInputStream(source) {
        override fun read(): Int = `in`.read().also {
            if (it >= 0) capture.write(it) else consumed = true
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int = `in`.read(b, off, len).also {
            if (it > 0) {
                capture.write(b, off, it)
            } else if (it == -1) {
                consumed = true
            }
        }

        override fun close() {
            consumed = true
            super.close()
        }
    }

    override val stream: InputStream
        get() = if (consumed) capture.toByteArray().inputStream() else tee

    override val payload: ByteBuffer
        get() {
            if (!consumed) {
                val buf = ByteArray(4096)
                while (tee.read(buf) != -1) { /* drains source, populating capture */ }
            }
            return ByteBuffer.wrap(capture.toByteArray())
        }

    override fun close() {
        tee.close()
    }

    override fun toString() = "<<captured-stream>>"
}
