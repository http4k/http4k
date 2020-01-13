package org.http4k.filter

import org.http4k.core.Body
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

enum class GzipCompressionMode(val compress: (Body) -> Body, val decompress: (Body) -> Body) {
    NON_STREAMING({ it.gzipped() }, { it.gunzipped() }),
    STREAMING({ it.gzippedStream() }, { it.gunzippedStream() })
}

fun Body.gzipped(): Body = if (payload.array().isEmpty()) Body.EMPTY
else ByteArrayOutputStream().run {
    GZIPOutputStream(this).use { it.write(payload.array()) }
    Body(ByteBuffer.wrap(toByteArray()))
}

fun Body.gunzipped(): Body = if (payload.array().isEmpty()) Body.EMPTY
else ByteArrayOutputStream().use {
    GZIPInputStream(ByteArrayInputStream(payload.array())).copyTo(it)
    Body(ByteBuffer.wrap(it.toByteArray()))
}

fun Body.gzippedStream(): Body = Body(GZippingInputStream(stream))

fun Body.gunzippedStream(): Body = if (length != null && length == 0L) {
    Body.EMPTY
} else {
    Body(GZIPInputStream(stream))
}

internal class GZippingInputStream(private val source: InputStream) : InputStream() {

    companion object {
        // see http://www.zlib.org/rfc-gzip.html#header-trailer
        private const val GZIP_MAGIC = 0x8b1f
        private val HEADER_DATA = byteArrayOf(
                GZIP_MAGIC.toByte(),
                (GZIP_MAGIC shr 8).toByte(),
                Deflater.DEFLATED.toByte(),
                0, 0, 0, 0, 0, 0, 0)
    }

    private enum class State {
        HEADER, DATA, FINALISE, TRAILER, DONE
    }

    private val deflater = Deflater(Deflater.DEFLATED, true)
    private val crc = CRC32()
    private var trailer: ByteArrayInputStream? = null
    private val header = ByteArrayInputStream(HEADER_DATA)

    private var stage = State.HEADER

    override fun read(): Int {
        val readBytes = ByteArray(1)
        var bytesRead = 0
        while (bytesRead == 0) {
            bytesRead = read(readBytes, 0, 1)
        }
        return if (bytesRead != -1) {
            readBytes[0].toInt().and(0xFF)
        } else {
            -1
        }
    }

    @Throws(IOException::class)
    override fun read(readBuffer: ByteArray, readOffset: Int, readLength: Int): Int = when (stage) {
        State.HEADER -> {
            val bytesRead = header.read(readBuffer, readOffset, readLength)
            if (header.available() == 0) {
                stage = State.DATA
            }
            bytesRead
        }
        State.DATA -> {
            val deflationBuffer = ByteArray(readLength)
            val bytesRead = source.read(deflationBuffer, 0, readLength)
            if (bytesRead <= 0) {
                stage = State.FINALISE
                deflater.finish()
                0
            } else {
                crc.update(deflationBuffer, 0, bytesRead)
                deflater.setInput(deflationBuffer, 0, bytesRead)
                var bufferBytesRead = 0
                while (!deflater.needsInput() && readLength - bufferBytesRead > 0) {
                    bufferBytesRead += deflater.deflate(readBuffer, readOffset + bufferBytesRead, readLength - bufferBytesRead, Deflater.NO_FLUSH)
                }
                bufferBytesRead
            }
        }
        State.FINALISE -> if (deflater.finished()) {
            stage = State.TRAILER
            val crcValue = crc.value.toInt()
            val totalIn = deflater.totalIn
            trailer = createTrailer(crcValue, totalIn)
            0
        } else {
            deflater.deflate(readBuffer, readOffset, readLength, Deflater.FULL_FLUSH)
        }
        State.TRAILER -> {
            val trailerStream = trailer ?: error("Trailer stream is null in trailer stage")
            val bytesRead = trailerStream.read(readBuffer, readOffset, readLength)
            if (trailerStream.available() == 0) {
                stage = State.DONE
            }
            bytesRead
        }
        State.DONE -> -1
    }

    private fun createTrailer(crcValue: Int, totalIn: Int) =
            ByteArrayInputStream(byteArrayOf(
                    (crcValue shr 0).toByte(),
                    (crcValue shr 8).toByte(),
                    (crcValue shr 16).toByte(),
                    (crcValue shr 24).toByte(),
                    (totalIn shr 0).toByte(),
                    (totalIn shr 8).toByte(),
                    (totalIn shr 16).toByte(),
                    (totalIn shr 24).toByte()))

    @Throws(IOException::class)
    override fun close() {
        source.close()
        deflater.end()
        trailer?.close()
        header.close()
    }

    init {
        crc.reset()
    }
}