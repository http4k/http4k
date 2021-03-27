package org.http4k.multipart

import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset

internal class TokenBoundedInputStream @JvmOverloads constructor(inputStream: InputStream, bufSize: Int, private val maxStreamLength: Int = -1) : CircularBufferedInputStream(inputStream, bufSize) {

    /**
     * Consumes all bytes up to and including the matched endOfToken bytes.
     * Fills the buffer with all bytes excluding the endOfToken bytes.
     * Returns the number of bytes inserted into the buffer.
     *
     * @param endOfToken bytes that indicate the end of this token
     * @param buffer     fills this buffer with bytes _excluding_ the endOfToken
     * @param encoding   Charset for formatting error messages
     * @return number of bytes inserted into buffer
     * @throws IOException
     */

    fun getBytesUntil(endOfToken: ByteArray, buffer: ByteArray, encoding: Charset): Int {
        var bufferIndex = 0
        val bufferLength = buffer.size

        var b: Int
        while (true) {
            b = readFromStream()
            if (b < 0) {
                throw TokenNotFoundException(
                    "Reached end of stream before finding Token <<${String(endOfToken, encoding)}>>. Last ${endOfToken.size} bytes read were <<${getBytesRead(endOfToken, buffer, bufferIndex, encoding)}>>")
            }
            if (bufferIndex >= bufferLength) {
                throw TokenNotFoundException("Didn't find end of Token <<${String(endOfToken, encoding)}>> within $bufferLength bytes")
            }
            val originalB = (b and 0x0FF).toByte()
            if (originalB == endOfToken[0]) {
                mark(endOfToken.size)
                if (matchToken(endOfToken, b)) {
                    return bufferIndex
                }
                reset()
            }
            buffer[bufferIndex++] = originalB
        }
    }

    private fun getBytesRead(endOfToken: ByteArray, buffer: ByteArray, bufferIndex: Int, encoding: Charset): String {
        val index: Int
        val length: Int
        if (bufferIndex - endOfToken.size > 0) {
            index = bufferIndex - endOfToken.size
            length = endOfToken.size
        } else {
            index = 0
            length = bufferIndex
        }
        return String(buffer, index, length, encoding)
    }

    private fun matchToken(token: ByteArray, initialCharacter: Int): Boolean {
        var initialChar = initialCharacter
        var eotIndex = 0
        while (initialChar > -1 && initialChar.toByte() == token[eotIndex] && ++eotIndex < token.size) {
            initialChar = readFromStream()
        }
        return eotIndex == token.size
    }

    /**
     * Tries to match the token bytes at the current position. Only consumes bytes
     * if there is a match, otherwise the stream is unaffected.
     *
     * @param token The token being matched
     * @return true if the token is found (and the bytes have been consumed),
     * false if it isn't found (and the stream is unchanged)
     */

    fun matchInStream(token: ByteArray): Boolean {
        mark(token.size)

        if (matchToken(token, readFromStream())) return true

        reset()
        return false
    }

    /**
     * returns a single byte from the Stream until the token is found. When the token is found,
     * -2 will be returned. The token will be consumed.
     *
     * @param token bytes that indicate the end of this token
     * @return the next byte in the stream, -1 if the underlying stream has finished,
     * or -2 if the token is found. The token is consumed when it is matched.
     */

    fun readByteFromStreamUnlessTokenMatched(token: ByteArray): Int {
        val b = readFromStream()
        if (b.toByte() == token[0]) {
            mark(token.size)

            if (matchToken(token, b)) return -2

            reset()
        }
        return b
    }

    private fun readFromStream(): Int {
        if (maxStreamLength > -1 && cursor >= maxStreamLength) throw StreamTooLongException("Form contents was longer than $maxStreamLength bytes")
        return read()
    }

    fun currentByteIndex(): Long = cursor
}
