package org.http4k.fanbelt.load

import org.http4k.core.ContentEncoding
import org.http4k.core.Method.GET
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.cookie.cookie
import org.http4k.core.patched.Brotli
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import sheets.domain.ViewerId
import sheets.web.viewerCookie
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

class Watcher(val node: Int, handler: PolyHandler, val viewer: ViewerId, path: String, private val onPatch: (String) -> Unit = {}) : Sse {
    override val connectRequest = Request(GET, path).cookie(viewerCookie(viewer))

    val patches = AtomicLong()
    val bytes = AtomicLong()
    val brotliBytes = AtomicLong()
    val brotliNanos = AtomicLong()
    private val brotli = ContentEncoding.Brotli().encoder(object : OutputStream() {
        override fun write(b: Int) {
            brotliBytes.incrementAndGet()
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            brotliBytes.addAndGet(len.toLong())
        }
    })
    private val seen = ConcurrentHashMap<String, Long>()
    private val closing = CopyOnWriteArrayList<() -> Unit>()

    init {
        val sse = checkNotNull(handler.sse) { "the node serves no streams" }
        sse(connectRequest).consumer(this)
    }

    override fun send(message: SseMessage) = apply {
        if (message is SseMessage.Event) {
            val text = message.toMessage()
            patches.incrementAndGet()
            bytes.addAndGet(text.length.toLong())
            compress(text)
            onPatch(text)
            val now = System.nanoTime()
            MARKER.findAll(text).forEach { seen.putIfAbsent(it.groupValues[1], now) }
        }
    }

    private fun compress(text: String) = synchronized(brotli) {
        val started = System.nanoTime()
        brotli.write(text.toByteArray())
        brotli.flush()
        brotliNanos.addAndGet(System.nanoTime() - started)
    }

    override fun close() = closing.forEach { it() }

    override fun onClose(fn: () -> Unit) = apply { closing += fn }

    fun sawAt(marker: String): Long? = seen[marker]
}

private val MARKER = Regex("""value="(v\d+)"""")
