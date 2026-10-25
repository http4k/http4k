package org.http4k.core

import org.http4k.core.ContentEncodingName.Companion.DEFLATE
import org.http4k.core.ContentEncodingName.Companion.GZIP
import org.http4k.filter.GZIPOutputStreamWith
import org.http4k.lens.ACCEPT_ENCODING
import org.http4k.lens.Header
import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.util.zip.Deflater.DEFAULT_COMPRESSION
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

/**
 * A content-coding which can be negotiated by the Compression filters. The encoder must emit everything written so
 * far when flushed, so that it can be used for streaming (eg. SSE). The filters take care of empty bodies, headers,
 * decompression size limits and decoding failures, so the encoder/decoder are just the raw codec streams.
 */
class ContentEncoding(
    val name: ContentEncodingName,
    val encoder: (OutputStream) -> OutputStream,
    val decoder: (InputStream) -> InputStream
) {
    companion object
}

/**
 * The client's preferred encoding (by "accept-encoding") from those offered, in order of server preference.
 */
fun Request.preferredEncoding(offered: List<ContentEncoding>): ContentEncoding? =
    runCatching { Header.ACCEPT_ENCODING(this) }.getOrNull()
        ?.preferred(offered, { range, name -> range.matches(name) }, ContentEncoding::name)

fun ContentEncoding.Companion.Gzip(level: Int = DEFAULT_COMPRESSION) = ContentEncoding(
    GZIP,
    { GZIPOutputStreamWith(it, level, syncFlush = true) },
    ::GZIPInputStream
)

fun ContentEncoding.Companion.Deflate(level: Int = DEFAULT_COMPRESSION) = ContentEncoding(
    DEFLATE,
    { object : DeflaterOutputStream(it, true) { init { def.setLevel(level) } } },
    ::inflating
)

// some servers send raw deflate rather than the zlib format the spec requires, so detect the zlib header (RFC 1950)
private fun inflating(input: InputStream): InputStream {
    val source = PushbackInputStream(input, 2)
    val header = source.readNBytes(2).also(source::unread)
    return object : InflaterInputStream(source, Inflater(!header.isZlibHeader())) {
        override fun close() {
            super.close()
            inf.end()
        }
    }
}

private fun ByteArray.isZlibHeader() =
    size == 2 && (this[0].toInt() and 0x0f) == 8 && ((this[0].toInt() and 0xff) * 256 + (this[1].toInt() and 0xff)) % 31 == 0

/**
 *  See https://www.iana.org/assignments/http-parameters/http-parameters.xhtml#content-coding
 */
@JvmInline
value class ContentEncodingName(val value: String) {
    // Generated from https://www.iana.org/assignments/http-parameters/content-coding.csv
    @Suppress("unused")
    companion object {
        /** AES-GCM encryption with a 128-bit content encryption key */
        val AES128GCM = ContentEncodingName("aes128gcm")

        /** Brotli Compressed Data Format */
        val BR = ContentEncodingName("br")

        /** UNIX "compress" data format [Welch, T., "A Technique for High Performance Data Compression", IEEE Computer 17(6), June 1984.] */
        val COMPRESS = ContentEncodingName("compress")

        /** "Dictionary-Compressed Brotli" data format. */
        val DCB = ContentEncodingName("dcb")

        /** "Dictionary-Compressed Zstandard" data format. */
        val DCZ = ContentEncodingName("dcz")

        /** "deflate" compressed data (RFC1951) inside the "zlib" data format ([RFC1950]) */
        val DEFLATE = ContentEncodingName("deflate")

        /** W3C Efficient XML Interchange */
        val EXI = ContentEncodingName("exi")

        /** GZIP file format (RFC1952) */
        val GZIP = ContentEncodingName("gzip")

        /** Reserved */
        val IDENTITY = ContentEncodingName("identity")

        /** Network Transfer Format for Java Archives */
        val PACK200_GZIP = ContentEncodingName("pack200-gzip")

        /** Deprecated (alias for compress) */
        val X_COMPRESS = ContentEncodingName("x-compress")

        /** Deprecated (alias for gzip) */
        val X_GZIP = ContentEncodingName("x-gzip")

        /** A stream of bytes compressed using the Zstandard protocol with a Window_Size of not more than 8 MB. */
        val ZSTD = ContentEncodingName("zstd")
    }
}
