package org.http4k.filter

import com.aayushatharva.brotli4j.decoder.Decoder
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.greaterThan
import org.http4k.core.Brotli
import org.http4k.core.ContentEncoding
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import kotlin.random.Random

class BrotliTest : ContentEncodingContract(ContentEncoding.Brotli(), { Decoder.decompress(it).decompressedData }) {

    @Test
    fun `a repeat further back than the window is not found`() {
        val block = Random(1).nextBytes(100_000)
        val repeatedPastAGap = block + Random(2).nextBytes(100_000) + block

        fun encodedSize(encoding: ContentEncoding) =
            ByteArrayOutputStream().also { out -> encoding.encoder(out).use { it.write(repeatedPastAGap) } }.size()

        assertThat(encodedSize(ContentEncoding.Brotli(window = 16)), greaterThan(encodedSize(ContentEncoding.Brotli(window = 22)) + 90_000))
    }
}
