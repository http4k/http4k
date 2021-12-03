package org.http4k.filter

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.asString
import org.http4k.core.Body
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.random.Random

class GzipBodyTest {

    @Nested
    inner class InMemoryGzip {

        @Test
        fun `a round-trip of a body works`() {
            assertThat(Body("foo").gzipped().body.gunzipped(), equalTo(Body("foo")))
        }

        @Test
        fun `the content-encoding for a compressed body is correct`() {
            assertThat(Body("foo").gzipped().contentEncoding, equalTo("gzip"))
        }

        @Test
        fun `the content-encoding for an unmodified body is correct`() {
            assertThat(Body.EMPTY.gzipped().contentEncoding, absent())
        }
    }

    @Nested
    inner class StreamingGzip {
        @Test
        fun `a gzipped body can be decompressed by the standard library`() {
            val gzipped = Body("foo").gzippedStream()

            val decompressedOutput = InputStreamReader(GZIPInputStream(gzipped.body.stream), Charsets.UTF_8).use {
                it.readText()
            }

            assertThat(decompressedOutput, equalTo("foo"))
        }

        @Test
        fun `a large randomised gzipped body can be decompressed by the standard library`() {
            val expectedBody = randomContent(512 * 1024)
            val gzipped = Body(expectedBody).gzippedStream()

            val decompressedOutput = InputStreamReader(GZIPInputStream(gzipped.body.stream), Charsets.UTF_8).use {
                it.readText()
            }

            assertThat(decompressedOutput, equalTo(expectedBody))
        }

        private fun randomContent(characterCount: Int): String =
            (('a'..'z') + ('A'..'Z') + ('0'..'9') + ' ').let { characterPool ->
                (0..characterCount)
                    .map { Random.nextInt(0, characterPool.size) }
                    .map { characterPool[it] }
                    .joinToString("")
            }

        @Test
        fun `a body gzipped by the standard library can be decompressed`() {
            val compressedBody = ByteArrayOutputStream().run {
                GZIPOutputStream(this).use { it.write("foo".toByteArray(Charsets.UTF_8)) }
                Body(ByteBuffer.wrap(toByteArray()))
            }

            val gunzipped = compressedBody.gunzippedStream()

            assertThat(gunzipped.payload.asString(), equalTo("foo"))
        }

        @Test
        fun `a empty in-memory body can be decompressed`() {
            val compressedBody = Body.EMPTY

            val gunzipped = compressedBody.gunzippedStream()

            assertThat(gunzipped.payload.asString(), equalTo(""))
        }

        @Test
        fun `a empty stream body can be decompressed`() {
            val compressedBody = Body("".byteInputStream(Charsets.UTF_8))

            val gunzipped = compressedBody.gunzippedStream()

            assertThat(gunzipped.payload.asString(), equalTo(""))
        }

        @Test
        fun `a round-trip of an empty body works`() {
            assertThat(Body.EMPTY.gzippedStream().body.gunzippedStream(), equalTo(Body.EMPTY))
        }

        @Test
        fun `a round-trip of a memory body works`() {
            assertThat(Body("foo").gzippedStream().body.gunzippedStream(), equalTo(Body("foo")))
        }

        @Test
        fun `a round-trip of a streaming body works`() {
            assertThat(Body("foo".byteInputStream(Charsets.UTF_8)).gzippedStream().body.gunzippedStream().payload,
                equalTo(Body("foo").payload))
        }

        @Test
        fun `the content-encoding for a compressed body is correct`() {
            assertThat(Body("foo".byteInputStream(Charsets.UTF_8)).gzippedStream().contentEncoding, equalTo("gzip"))
        }

        @Test
        fun `the content-encoding for an unmodified body is correct`() {
            assertThat(Body("".byteInputStream(Charsets.UTF_8)).gzipped().contentEncoding, absent())
        }
    }
}
