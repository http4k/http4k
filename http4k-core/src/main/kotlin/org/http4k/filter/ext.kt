package org.http4k.filter

import org.http4k.core.Body
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

fun Body.gzipped(): Body = if (payload.array().isEmpty()) Body.EMPTY
else ByteArrayOutputStream().let {
    GZIPOutputStream(it).let {
        it.write(payload.array())
        it.close()
    }
    Body(ByteBuffer.wrap(it.toByteArray()))
}

fun Body.gunzipped(): Body = if (payload.array().isEmpty()) Body.EMPTY
else ByteArrayOutputStream().let {
    GZIPInputStream(ByteArrayInputStream(payload.array())).copyTo(it, 4096)
    it.close()
    Body(ByteBuffer.wrap(it.toByteArray()))
}