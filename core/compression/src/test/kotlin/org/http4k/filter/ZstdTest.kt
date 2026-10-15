package org.http4k.filter

import com.github.luben.zstd.Zstd
import com.github.luben.zstd.ZstdOutputStream
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.ContentEncoding
import org.http4k.core.Zstd
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

class ZstdTest : ContentEncodingContract(ContentEncoding.Zstd(), { Zstd.decompress(it, 1024) }) {

    @Test
    fun `decodes a frame within the 8MB window allowed for http`() {
        assertThat(String(ContentEncoding.Zstd().decoder(frameWithWindowLog(23)).readBytes()), equalTo("foobar"))
    }

    @Test
    fun `rejects a frame requiring a window larger than the 8MB allowed for http`() {
        assertThat({ ContentEncoding.Zstd().decoder(frameWithWindowLog(24)).readBytes() }, throws<IOException>())
    }

    private fun frameWithWindowLog(windowLog: Int) = ByteArrayOutputStream()
        .also { out -> ZstdOutputStream(out).setWindowLog(windowLog).use { it.write("foobar".toByteArray()) } }
        .let { ByteArrayInputStream(it.toByteArray()) }
}
