package org.http4k.core

import com.aayushatharva.brotli4j.Brotli4jLoader
import com.aayushatharva.brotli4j.decoder.BrotliInputStream
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream
import com.aayushatharva.brotli4j.encoder.Encoder
import org.http4k.core.ContentEncodingName.Companion.BR

/**
 * Brotli content-encoding. Quality ranges from 0 (fastest) to 11 (smallest).
 */
fun ContentEncoding.Companion.Brotli(quality: Int = 4): ContentEncoding {
    Brotli4jLoader.ensureAvailability()
    val parameters = Encoder.Parameters().setQuality(quality)
    return ContentEncoding(BR, { BrotliOutputStream(it, parameters) }, { BrotliInputStream(it).apply { enableEagerOutput() } })
}
