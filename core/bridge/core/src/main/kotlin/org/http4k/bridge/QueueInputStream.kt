package org.http4k.bridge

import java.io.IOException
import java.io.InputStream
import java.util.concurrent.LinkedBlockingQueue

/**
 * A blocking InputStream fed by chunks pushed from a reactive (push-based) source, letting a
 * blocking http4k handler consume a body that the underlying framework delivers asynchronously.
 *
 * The source calls [push]/[end]/[fail]; the http4k handler (on a worker thread) pulls via [read].
 * [requestMore] is invoked whenever the consumer needs the next chunk, so the source only delivers
 * when asked - that is the backpressure. The blocking [read] must never run on the same thread
 * that calls [push], or it deadlocks.
 */
class QueueInputStream(private val requestMore: () -> Unit) : InputStream() {
    private val queue = LinkedBlockingQueue<Signal>()
    private var current = ByteArray(0)
    private var pos = 0
    private var ended = false

    fun push(bytes: ByteArray) = queue.put(Chunk(bytes))
    fun end() = queue.put(End)
    fun fail(cause: Throwable) = queue.put(Failure(cause))

    override fun read(): Int {
        val one = ByteArray(1)
        return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xFF
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        if (!ensureAvailable()) return -1
        val count = minOf(len, current.size - pos)
        System.arraycopy(current, pos, b, off, count)
        pos += count
        return count
    }

    private fun ensureAvailable(): Boolean {
        while (pos >= current.size) {
            if (ended) return false
            requestMore()
            when (val signal = queue.take()) {
                is Chunk -> {
                    current = signal.bytes
                    pos = 0
                }

                End -> {
                    ended = true
                    return false
                }

                is Failure -> throw IOException(signal.cause)
            }
        }
        return true
    }

    private sealed interface Signal
    private class Chunk(val bytes: ByteArray) : Signal
    private object End : Signal
    private class Failure(val cause: Throwable) : Signal
}
