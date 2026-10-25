package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Brotli
import org.http4k.core.ContentEncoding
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Deflate
import org.http4k.core.Method.GET
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Zstd
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.sse.SseMessage.Data
import org.http4k.sse.SseResponse
import org.http4k.sse.then
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

class StreamingSseCompressionTest : PortBasedTest {

    @Test
    fun `streams brotli compressed sse message by message`() = assertStreamsMessageByMessage(ContentEncoding.Brotli())

    @Test
    fun `streams zstd compressed sse message by message`() = assertStreamsMessageByMessage(ContentEncoding.Zstd())

    @Test
    fun `streams deflate compressed sse message by message`() = assertStreamsMessageByMessage(ContentEncoding.Deflate())

    private fun assertStreamsMessageByMessage(encoding: ContentEncoding) {
        val firstMessageReceived = CountDownLatch(1)
        val sse = ServerFilters.SseCompression(encoding).then {
            SseResponse(OK) { sse ->
                sse.send(Data("first"))
                firstMessageReceived.await(5, SECONDS)
                sse.send(Data("second"))
                sse.close()
            }
        }

        PolyHandler(sse = sse).asServer(Jetty(0)).start().use { server ->
            val response = JavaHttpClient(responseBodyMode = Stream)(
                Request(GET, "http://localhost:${server.port()}/")
                    .header("Accept", TEXT_EVENT_STREAM.value)
                    .header("Accept-Encoding", encoding.name.value)
            )
            assertThat(response.header("content-encoding"), equalTo(encoding.name.value))

            val decoded = encoding.decoder(response.body.stream)
            val received = ByteArrayOutputStream()
            assertTimeoutPreemptively(Duration.ofSeconds(5)) {
                while (!received.toString().contains("first")) received.write(decoded.read())
            }
            firstMessageReceived.countDown()

            assertThat(String(decoded.readBytes()), containsSubstring("second"))
        }
    }
}
