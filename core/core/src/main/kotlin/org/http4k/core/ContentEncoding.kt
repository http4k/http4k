package org.http4k.core

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
