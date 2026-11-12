/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.core.patched

import com.aayushatharva.brotli4j.Brotli4jLoader
import com.aayushatharva.brotli4j.decoder.BrotliInputStream
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream
import com.aayushatharva.brotli4j.encoder.Encoder
import org.http4k.core.ContentEncoding
import org.http4k.core.ContentEncodingName.Companion.BR

fun ContentEncoding.Companion.Brotli(quality: Int = 4, window: Int = 18): ContentEncoding {
    Brotli4jLoader.ensureAvailability()
    val parameters = Encoder.Parameters().setQuality(quality).setWindow(window)
    return ContentEncoding(BR, { BrotliOutputStream(it, parameters) }, { BrotliInputStream(it).apply { enableEagerOutput() } })
}
