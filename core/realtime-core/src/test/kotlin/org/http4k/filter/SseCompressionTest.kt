package org.http4k.filter

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentEncoding
import org.http4k.core.ContentEncodingName.Companion.GZIP
import org.http4k.core.Gzip
import org.http4k.core.Method.GET
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.hamkrest.hasHeader
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.SseResponse
import org.http4k.sse.then
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class SseCompressionTest {

    private val compressing = ServerFilters.SseCompression(ContentEncoding.Gzip())

    @Test
    fun `selects the encoding preferred by the client`() {
        val response = compressing.then { SseResponse { } }(Request(GET, "").header("accept-encoding", "gzip"))

        assertThat(response.contentEncoding?.name, equalTo(GZIP))
    }

    @Test
    fun `does not encode when the client accepts none of the encodings`() {
        val response = compressing.then { SseResponse { } }(Request(GET, "").header("accept-encoding", "br"))

        assertThat(response.contentEncoding, absent())
    }

    @Test
    fun `encoding survives routing and later changes to the consumer`() {
        val app = DebuggingFilters.PrintSseResponse(PrintStream(ByteArrayOutputStream()))
            .then(compressing)
            .then(sse("/stream" bind { SseResponse { } }))

        val response = app(Request(GET, "/stream").header("accept-encoding", "gzip"))

        assertThat(response.contentEncoding?.name, equalTo(GZIP))
    }

    @Test
    fun `poly compression compresses both http and sse`() {
        val app = PolyFilters.Compression(ContentEncoding.Gzip()).then(
            PolyHandler(http = { Response(OK).body("foobar") }, sse = { SseResponse { } })
        )
        val request = Request(GET, "").header("accept-encoding", "gzip")

        assertThat(app.http!!(request), hasHeader("content-encoding", "gzip"))
        assertThat(app.sse!!(request).contentEncoding?.name, equalTo(GZIP))
    }
}
