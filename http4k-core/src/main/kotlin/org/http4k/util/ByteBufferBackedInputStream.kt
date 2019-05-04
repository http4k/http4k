package org.http4k.util

import java.io.InputStream
import java.nio.ByteBuffer

internal class ByteBufferBackedInputStream(private val buf: ByteBuffer) : InputStream() {
    override fun read() = if (buf.hasRemaining()) buf.get().toInt() else -1

    override fun read(bytes: ByteArray, off: Int, len: Int): Int = Math.min(len, buf.remaining()).apply {
        buf.get(bytes, off, this)
    }
}