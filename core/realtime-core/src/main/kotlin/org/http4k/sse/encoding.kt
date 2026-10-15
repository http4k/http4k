package org.http4k.sse

import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Headers
import org.http4k.core.Request
import org.http4k.lens.ACCEPT_ENCODING
import org.http4k.lens.CONTENT_ENCODING
import org.http4k.lens.Header
import java.io.BufferedOutputStream
import java.io.FilterOutputStream
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * For server implementations: the headers to add when the event stream is encoded.
 */
val SseResponse.encodingHeaders: Headers
    get() = contentEncoding
        ?.let { listOf(Header.CONTENT_ENCODING.meta.name to it.name.value, "vary" to Header.ACCEPT_ENCODING.meta.name) }
        ?: emptyList()

/**
 * For server implementations using [streamTo]: the event stream headers which a native SSE API would otherwise set.
 */
val streamHeaders: Headers = listOf("Content-Type" to TEXT_EVENT_STREAM.toHeaderValue(), "Cache-Control" to "no-cache")

/**
 * For server implementations: wraps the raw event stream in the content encoding (if any), so each flush sends a
 * compressed chunk. Closing the result finishes the encoding but leaves the raw stream open for the server to complete.
 */
fun SseResponse.encode(raw: OutputStream): OutputStream {
    val unclosable = object : FilterOutputStream(raw) {
        override fun write(b: ByteArray, off: Int, len: Int) = out.write(b, off, len)
        override fun close() = flush()
    }
    return contentEncoding?.let { BufferedOutputStream(it.encoder(unclosable)) } ?: unclosable
}

/**
 * For server implementations which can only write to a blocking stream: runs the consumer against the (encoded) raw
 * output, returning once the stream is closed. The raw stream is left for the server to complete.
 */
fun SseResponse.streamTo(request: Request, raw: OutputStream) {
    val output = encode(raw)
    val lock = ReentrantLock()
    val closed = CountDownLatch(1)

    val sse = object : PushAdaptingSse(request) {
        override fun send(message: SseMessage) = apply {
            lock.withLock {
                runCatching {
                    output.write(message.toMessage().toByteArray())
                    output.flush()
                }
            }.onFailure { close() }
        }

        override fun close() {
            lock.withLock {
                if (closed.count == 0L) return
                runCatching { output.close() }
                closed.countDown()
            }
            triggerClose()
        }
    }

    try {
        consumer(sse)
    } catch (_: Exception) {
        sse.close()
    }
    closed.await()
}
