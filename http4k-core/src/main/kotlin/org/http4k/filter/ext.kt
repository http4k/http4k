package org.http4k.filter

import org.http4k.core.Body
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

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