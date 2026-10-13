package org.http4k.webdriver.datastar

import org.http4k.sse.SseMessage
import org.http4k.sse.chunkedSseSequence
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream

internal class LiveStream(private val input: InputStream) : Closeable {
    private val pending = ByteArrayOutputStream()

    fun received(): List<SseMessage> {
        val before = pending.size()
        while (input.available() > 0) {
            val chunk = ByteArray(input.available())
            val read = input.read(chunk)
            if (read < 0) break
            pending.write(chunk, 0, read)
        }
        if (pending.size() == before) return emptyList()
        val bytes = pending.toByteArray()
        val end = bytes.endOfLastEvent()
        if (end == 0) return emptyList()
        pending.reset()
        pending.write(bytes, end, bytes.size - end)
        return bytes.inputStream(0, end).chunkedSseSequence().toList()
    }

    override fun close() = input.close()
}

private fun ByteArray.endOfLastEvent(): Int {
    for (i in lastIndex downTo 1) {
        if (this[i] == LF && precededByLineBreak(i)) return i + 1
    }
    return 0
}

private fun ByteArray.precededByLineBreak(i: Int) =
    this[i - 1] == LF || (this[i - 1] == CR && getOrNull(i - 2) == LF)

private const val LF = '\n'.code.toByte()
private const val CR = '\r'.code.toByte()
