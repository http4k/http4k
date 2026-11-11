package org.http4k.testing

import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.accept
import org.http4k.sse.PushAdaptingSse
import org.http4k.sse.SseMessage
import org.http4k.sse.SseResponse
import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

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
 * Converts a [PolyHandler] into an [HttpHandler], as a server would serve it. A request that accepts
 * `text/event-stream` goes to the SSE handler, and its messages are streamed in the response body as
 * they are sent; any other request, or one that no SSE route handles, goes to the HTTP handler.
 * Closing the body closes the SSE connection.
 */
fun PolyHandler.toHttpHandler(): HttpHandler {
    val streams = sse ?: return http ?: error("PolyHandler has neither http nor sse")
    return { request ->
        val stream = if (request.accept()?.accepts(TEXT_EVENT_STREAM) == true) streams(request) else null
        val pages = http
        when {
            stream?.handled == true -> stream.overHttp(request)
            pages != null -> pages(request)
            stream != null -> Response(stream.status).headers(stream.headers)
            else -> error("http not implemented")
        }
    }
}

private fun SseResponse.overHttp(request: Request): Response {
    val messages = MessageStream()
    val sse = object : PushAdaptingSse(request) {
        override fun send(message: SseMessage) = apply { messages.push(message.toMessage().toByteArray()) }

        override fun close() = messages.end()
    }
    messages.onClose = sse::triggerClose
    consumer(sse)
    return Response(status).headers(headers)
        .with(Header.CONTENT_TYPE of TEXT_EVENT_STREAM)
        .body(CapturingBody(messages))
}

/**
 * The bytes of the messages sent so far, read as they arrive. Reading blocks until a message is sent
 * or the stream ends, and [available] counts only what has already been sent.
 */
private class MessageStream : InputStream() {
    private val queue = LinkedBlockingQueue<ByteArray>()
    private var chunk = ByteArray(0)
    private var position = 0
    private val ended = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    var onClose: () -> Unit = {}

    fun push(bytes: ByteArray) {
        if (!ended.get()) queue.add(bytes)
    }

    fun end() {
        if (ended.compareAndSet(false, true)) queue.add(END)
    }

    override fun available() = chunk.size - position + queue.sumOf { it.size }

    override fun read() = if (refilled()) chunk[position++].toInt() and 0xff else -1

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (!refilled()) return -1
        val count = minOf(length, chunk.size - position)
        System.arraycopy(chunk, position, buffer, offset, count)
        position += count
        return count
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        end()
        onClose()
    }

    private fun refilled(): Boolean {
        if (position < chunk.size) return true
        if (closed.get()) return false
        val next = queue.take()
        if (next === END) {
            queue.add(END)
            return false
        }
        chunk = next
        position = 0
        return true
    }

    private companion object {
        val END = ByteArray(0)
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
