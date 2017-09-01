package org.http4k.multipart.stream

import java.io.IOException
import java.io.InputStream
import java.nio.InvalidMarkException

open class CircularBufferedInputStream(private val inputStream: InputStream, maxExpectedBufSize: Int) : InputStream() {
    private val bufferSize: Int = Integer.highestOneBit(maxExpectedBufSize) * 2
    private val bufferIndexMask: Long = (bufferSize - 1).toLong()
    private val buffer: ByteArray = ByteArray(bufferSize)

    protected var cursor: Long = 0
    private var rightBounds: Long = 0
    private var leftBounds: Long = 0
    private var readLimit: Long = 0
    private var markInvalid: Boolean = false
    private var EOS: Boolean = false

    @Throws(IOException::class)
    override fun read(): Int {
        dumpState(">>> READ")

        if (EOS) {
            return -1
        }
        val result = read1()

        dumpState("<<< READ")

        return result
    }

    private fun read1(): Int {
        while (cursor == rightBounds) {
            if (!readMore()) return -1
        }
        return BitFiddling.getAnInt(buffer[(cursor++ and bufferIndexMask).toInt()], 0x0FF)
    }


    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (off < 0 || len < 0 || len > b.size - off) {
            throw IndexOutOfBoundsException()
        } else if (len == 0) {
            return 0
        }

        if (EOS) {
            return -1
        }

        for (i in 0 until len) {
            val result = read1()
            if (result == -1) {
                return i
            }
            b[i + off] = result.toByte()
        }

        return len
    }

    @Throws(IOException::class)
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

    @Throws(IOException::class)
    override fun available(): Int {
        return (rightBounds - cursor).toInt()
    }

    override fun markSupported(): Boolean {
        return true
    }

    @Synchronized
    @Throws(IOException::class)
    override fun reset() {
        dumpState(">>> RESET")

        if (markInvalid) {
            // The mark has been moved because you have read past your readlimit
            throw InvalidMarkException()
        }
        cursor = leftBounds
        readLimit = 0
        markInvalid = false

        dumpState("<<< RESET")
    }

    @Synchronized override fun mark(readlimit: Int) {
        dumpState(">>> MARK")

        if (readlimit > bufferSize) {
            throw ArrayIndexOutOfBoundsException(String.format("Readlimit (%d) cannot be bigger than buffer size (%d)", readlimit, bufferSize))
        }
        leftBounds = cursor
        markInvalid = false
        this.readLimit = readlimit.toLong()

        dumpState("<<< MARK")
    }

    private fun dumpState(description: String) {
        if (DEBUG) {
            println(description)
            println(
                "l=" + leftBounds + "(" + (leftBounds and bufferIndexMask) + ") " +
                    "c=" + cursor + "(" + (cursor and bufferIndexMask) + ") " +
                    "r=" + rightBounds + "(" + (rightBounds and bufferIndexMask) + ") '" +
                    "rl=" + readLimit + " " +
                    buffer[(cursor and bufferIndexMask).toInt()].toChar() + "'")
            for (b in buffer) {
                print(b.toChar())
            }
            println()
        }
    }

    companion object {
        private val DEBUG = false
    }

}
