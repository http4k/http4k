package org.http4k.multipart

import java.io.InputStream
import java.nio.InvalidMarkException

internal open class CircularBufferedInputStream(private val inputStream: InputStream, maxExpectedBufSize: Int) : InputStream() {
    private val bufferSize = Integer.highestOneBit(maxExpectedBufSize) * 2
    private val bufferIndexMask = (bufferSize - 1).toLong()
    private val buffer = ByteArray(bufferSize)

    protected var cursor = 0L
    private var rightBounds = 0L
    private var leftBounds = 0L
    private var readLimit = 0L
    private var markInvalid = false
    private var EOS = false

    override fun read(): Int = if (EOS) -1 else read1()

    private fun read1(): Int {
        while (cursor == rightBounds) {
            if (!readMore()) return -1
        }
        return BitFiddling.getAnInt(buffer[(cursor++ and bufferIndexMask).toInt()], 0x0FF)
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (off < 0 || len < 0 || len > b.size - off) throw IndexOutOfBoundsException() else if (len == 0) return 0

        if (EOS) return -1

        for (i in 0 until len) {
            val result = read1()
            if (result == -1) return i
            b[i + off] = result.toByte()
        }

        return len
    }

    private fun readMore(): Boolean {
        val rightIndex = rightBounds and bufferIndexMask
        val leftIndex = leftBounds and bufferIndexMask

        val readThisManyBytes = if (leftIndex > rightIndex) (leftIndex - rightIndex).toInt() else (buffer.size - rightIndex).toInt()

        val readBytes = inputStream.read(
            buffer,
            rightIndex.toInt(),
            readThisManyBytes
        )

        if (readBytes < 0) {
            EOS = true
            return false
        }
        rightBounds += readBytes.toLong()

        // move mark if past readLimit
        if (cursor - leftBounds > readLimit) {
            leftBounds = cursor
            readLimit = 0
            markInvalid = true
        }

        return true
    }

    override fun available(): Int = (rightBounds - cursor).toInt()

    override fun markSupported(): Boolean = true

    @Synchronized
    override fun reset() {
        // The mark has been moved because you have read past your readlimit
        if (markInvalid) throw InvalidMarkException()

        cursor = leftBounds
        readLimit = 0
        markInvalid = false
    }

    @Synchronized
    override fun mark(readlimit: Int) {
        if (readlimit > bufferSize) throw ArrayIndexOutOfBoundsException(String.format("Readlimit (%d) cannot be bigger than buffer size (%d)", readlimit, bufferSize))
        leftBounds = cursor
        markInvalid = false
        readLimit = readlimit.toLong()
    }
}
