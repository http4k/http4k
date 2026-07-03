package org.http4k.util

import java.io.IOException
import java.io.InputStream

class SizeLimitExceededException(maxSize: Long) : IOException("Stream exceeded maximum size of $maxSize bytes")

internal class SizeLimitedInputStream(private val delegate: InputStream, private val maxSize: Long) : InputStream() {
    private var count = 0L

    override fun read(): Int = delegate.read().also { if (it != -1) increment(1) }

    override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len).also { if (it != -1) increment(it) }

    private fun increment(n: Int) {
        count += n
        if (count > maxSize) throw SizeLimitExceededException(maxSize)
    }

    override fun close() = delegate.close()
}
