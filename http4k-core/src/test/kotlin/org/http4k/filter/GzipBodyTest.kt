package org.http4k.filter

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

class GzipBodyTest {

    @Nested
    inner class InMemoryGzip {

        @Test
        fun roundtrip() {
            assertThat(Body("foo").gzipped().gunzipped(), equalTo(Body("foo")))
        }

    }

    @Nested
    inner class StreamingGzip {
        @Test
        fun `a gzipped body can be decompressed by the standard library`() {
            val gzipped = Body("foo").gzippedStream()

            val decompressedOutput = InputStreamReader(GZIPInputStream(gzipped.stream), Charsets.UTF_8).use {
                it.readText()
            }

            assertThat(decompressedOutput, equalTo("foo"))
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
        fun `a empty body can be decompressed`() {
            val compressedBody = Body("")

            val gunzipped = compressedBody.gunzippedStream()

            assertThat(gunzipped.payload.asString(), equalTo(""))
        }

        @Test
        fun `a round-trip of an empty body works`() {
            assertThat(Body("").gzippedStream().gunzippedStream(), equalTo(Body("")))
        }

        @Test
        fun `a round-trip of a memory body works`() {
            assertThat(Body("foo").gzippedStream().gunzippedStream(), equalTo(Body("foo")))
        }

        @Test
        fun `a round-trip of a streaming body works`() {
            assertThat(Body("foo".byteInputStream(Charsets.UTF_8)).gzippedStream().gunzippedStream().payload,
                    equalTo(Body("foo").payload))
        }
    }

}
