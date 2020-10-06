package org.http4k.filter

import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.core.Response
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.PushbackInputStream
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

sealed class GzipCompressionMode(internal val compress: (Body) -> CompressionResult, internal val decompress: (Body) -> Body) {

    object Memory : GzipCompressionMode(Body::gzipped, Body::gunzipped)

    object Streaming : GzipCompressionMode(Body::gzippedStream, Body::gunzippedStream)
}

data class CompressionResult(
    val body: Body,
    val contentEncoding: String?
) {
    fun apply(request: Request): Request =
        (contentEncoding?.let {
            request.header("content-encoding", it)
        } ?: request)
            .body(body)

    fun apply(response: Response): Response =
        (contentEncoding?.let {
            response.header("content-encoding", it)
        } ?: response)
            .body(body)
}

fun Body.gzipped(): CompressionResult = if (payload.array().isEmpty())
    CompressionResult(Body.EMPTY, null)
else ByteArrayOutputStream().run {
    GZIPOutputStream(this).use { it.write(payload.array()) }
    CompressionResult(Body(ByteBuffer.wrap(toByteArray())), "gzip")
}

fun Body.gunzipped(): Body = if (payload.array().isEmpty()) Body.EMPTY
else ByteArrayOutputStream().use {
    GZIPInputStream(ByteArrayInputStream(payload.array())).copyTo(it)
    Body(ByteBuffer.wrap(it.toByteArray()))
}

fun Body.gzippedStream(): CompressionResult =
    sampleStream(stream,
        { CompressionResult(Body.EMPTY, null) },
        { compressedStream -> CompressionResult(Body(GZippingInputStream(compressedStream)), "gzip") })

fun Body.gunzippedStream(): Body = if (length != null && length == 0L) {
    Body.EMPTY
} else {
    sampleStream(stream,
        { Body.EMPTY },
        { compressedStream -> Body(GZIPInputStream(compressedStream)) })
}

private fun <T> sampleStream(sourceStream: InputStream, actionIfEmpty: () -> T, actionIfHasContent: (InputStream) -> T): T {
    val pushbackStream = PushbackInputStream(sourceStream)
    val firstByte = pushbackStream.read()
    return if (firstByte == -1) {
        actionIfEmpty()
    } else {
        pushbackStream.unread(firstByte)
        actionIfHasContent(pushbackStream)
    }
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
        private const val INITIAL_BUFFER_SIZE = 8192
    }

    private enum class State {
        HEADER, DATA, FINALISE, TRAILER, DONE
    }

    private val deflater = Deflater(Deflater.DEFLATED, true)
    private val crc = CRC32()
    private var trailer: ByteArrayInputStream? = null
    private val header = ByteArrayInputStream(HEADER_DATA)

    private var deflationBuffer: ByteArray = ByteArray(INITIAL_BUFFER_SIZE)
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
            if (!deflater.needsInput()) {
                deflatePendingInput(readBuffer, readOffset, readLength)
            } else {
                if (deflationBuffer.size < readLength) {
                    deflationBuffer = ByteArray(readLength)
                }

                val bytesRead = source.read(deflationBuffer, 0, readLength)
                if (bytesRead <= 0) {
                    stage = State.FINALISE
                    deflater.finish()
                    0
                } else {
                    crc.update(deflationBuffer, 0, bytesRead)
                    deflater.setInput(deflationBuffer, 0, bytesRead)
                    deflatePendingInput(readBuffer, readOffset, readLength)
                }
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

    private fun deflatePendingInput(readBuffer: ByteArray, readOffset: Int, readLength: Int): Int {
        var bytesCompressed = 0
        while (!deflater.needsInput() && readLength - bytesCompressed > 0) {
            bytesCompressed += deflater.deflate(readBuffer, readOffset + bytesCompressed, readLength - bytesCompressed, Deflater.NO_FLUSH)
        }
        return bytesCompressed
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

    override fun available(): Int {
        if (stage == State.DONE) {
            return 0
        }
        return 1
    }

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
