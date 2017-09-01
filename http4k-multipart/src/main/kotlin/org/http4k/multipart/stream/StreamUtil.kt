package org.http4k.multipart.stream

import org.http4k.multipart.exceptions.StreamTooLongException
import java.io.InputStream
import java.nio.charset.Charset

object StreamUtil {
    fun readStringFromInputStream(inputStream: InputStream, encoding: Charset, maxPartContentSize: Int): String {
        val bytes = ByteArray(maxPartContentSize)
        val length = readAllBytesFromInputStream(inputStream, maxPartContentSize, bytes)
        return String(bytes, 0, length, encoding)
    }

    private fun readAllBytesFromInputStream(inputStream: InputStream, maxLength: Int, bytes: ByteArray): Int {
        var length = 0

        while (true) {
            val count = inputStream.read(bytes, length, maxLength - length)
            if (count < 0) {
                inputStream.close()
                return length
            }
            if (length >= maxLength) {
                inputStream.close()
                throw StreamTooLongException("Part contents was longer than $maxLength bytes")
            }
            length += count
        }
    }
}
