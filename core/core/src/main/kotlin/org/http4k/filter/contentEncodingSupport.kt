package org.http4k.filter

import org.http4k.core.Body
import org.http4k.core.ContentEncoding
import org.http4k.core.ContentType
import org.http4k.core.HttpMessage
import org.http4k.core.Response
import org.http4k.core.StreamBody
import org.http4k.lens.CONTENT_ENCODING
import org.http4k.lens.Header
import org.http4k.util.SizeLimitedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * A Compression guard which only allows Responses whose content-type (ignoring parameters such as charset) is one of
 * the given types.
 */
fun compressibleContentTypes(vararg types: ContentType): (Response) -> Boolean {
    val compressible = types.map { it.value.lowercase() }.toSet()
    return { response -> runCatching { Header.CONTENT_TYPE(response) }.getOrNull()?.value?.lowercase() in compressible }
}

internal const val USE_COMPRESSION_FILTERS =
    "Use the negotiating Compression filters, which also support Brotli and Zstd (http4k-compression)"

internal const val USE_COMPRESSION_FILTERS_WITH_GUARD =
    "Use the negotiating Compression filters with a compressibleContentTypes guard, which also support Brotli and Zstd (http4k-compression)"

internal fun ContentEncoding.encoded(body: Body) = when {
    body.length == 0L -> CompressionResult(body, null)

    body is StreamBody -> CompressionResult(Body(EncodingInputStream(body.stream, encoder)), name.value)

    else -> CompressionResult(
        Body(ByteBuffer.wrap(ByteArrayOutputStream().also { out -> encoder(out).use { body.stream.copyTo(it) } }.toByteArray())),
        name.value
    )
}

@Suppress("UNCHECKED_CAST")
internal fun <T : HttpMessage> T.decodedWith(encodings: Array<out ContentEncoding>, maxSize: Long): T? =
    encodings.matching(this)?.let { removeHeader("content-encoding").removeHeader("content-length").body(it.decoded(body, maxSize)) as T }

private fun Array<out ContentEncoding>.matching(message: HttpMessage) =
    when (message.body.length) {
        0L -> null

        else -> runCatching { Header.CONTENT_ENCODING(message) }.getOrNull()
            ?.let { name -> firstOrNull { it.name == name } }
    }

private fun ContentEncoding.decoded(body: Body, maxSize: Long) =
    Body(SizeLimitedInputStream(DecodingInputStream(body.stream, decoder), maxSize))

internal class DecompressionFailure(cause: IOException) : IOException(cause)

private inline fun <T> failingAsDecompression(fn: () -> T): T = try {
    fn()
} catch (e: IOException) {
    throw DecompressionFailure(e)
}

private class DecodingInputStream(private val source: InputStream, decode: (InputStream) -> InputStream) : InputStream() {
    private val lazyDecoded = lazy { failingAsDecompression { decode(source) } }
    private val decoded by lazyDecoded

    override fun read() = failingAsDecompression { decoded.read() }
    override fun read(b: ByteArray, off: Int, len: Int) = failingAsDecompression { decoded.read(b, off, len) }
    override fun close() = if (lazyDecoded.isInitialized()) decoded.close() else source.close()
}

private class EncodingInputStream(private val source: InputStream, encoder: (OutputStream) -> OutputStream) : InputStream() {
    private val encodedBytes = ByteArrayOutputStream()
    private val encoding = encoder(encodedBytes)
    private val chunk = ByteArray(8192)
    private val single = ByteArray(1)
    private var pending = ByteArray(0)
    private var position = 0
    private var finished = false

    override fun read() = if (read(single, 0, 1) < 0) -1 else single[0].toInt() and 0xFF

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        while (position == pending.size) {
            if (finished) return -1
            when (val read = source.read(chunk)) {
                -1 -> encoding.close().also { finished = true }
                else -> encoding.write(chunk, 0, read)
            }
            if (encodedBytes.size() > 0) {
                pending = encodedBytes.toByteArray().also { encodedBytes.reset() }
                position = 0
            }
        }
        val count = minOf(len, pending.size - position)
        System.arraycopy(pending, position, b, off, count)
        position += count
        return count
    }

    override fun close() = source.close()
}
