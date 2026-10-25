package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentEncoding
import org.http4k.core.Deflate
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

class DeflateContentEncodingTest : ContentEncodingContract(ContentEncoding.Deflate(), { InflaterInputStream(it.inputStream()).readBytes() }) {

    @Test
    fun `decodes raw deflate without the zlib wrapper as sent by some servers`() {
        val raw = ByteArrayOutputStream()
            .also { out -> DeflaterOutputStream(out, Deflater(Deflater.DEFAULT_COMPRESSION, true)).use { it.write("foobar".toByteArray()) } }
            .toByteArray()

        assertThat(String(ContentEncoding.Deflate().decoder(raw.inputStream()).readBytes()), equalTo("foobar"))
    }
}
