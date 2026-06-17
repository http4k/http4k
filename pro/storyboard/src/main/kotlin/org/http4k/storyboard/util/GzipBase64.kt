/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

fun String.gzipBase64Encode(): String {
    if (isEmpty()) return ""
    val buffer = ByteArrayOutputStream()
    GZIPOutputStream(buffer).use { it.write(toByteArray(Charsets.UTF_8)) }
    return Base64.getEncoder().encodeToString(buffer.toByteArray())
}

fun String.gzipBase64Decode(): String {
    if (isEmpty()) return ""
    val bytes = Base64.getDecoder().decode(this)
    return GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes().toString(Charsets.UTF_8) }
}
