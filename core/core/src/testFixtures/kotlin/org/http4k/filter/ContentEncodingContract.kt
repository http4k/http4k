package org.http4k.filter

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.ContentEncoding
import org.http4k.core.Gzip
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.REQUEST_ENTITY_TOO_LARGE
import org.http4k.core.then
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

abstract class ContentEncodingContract(
    private val encoding: ContentEncoding,
    private val independentlyDecode: (ByteArray) -> ByteArray
) {
    private val name = encoding.name.value

    @Test
    fun `compresses response when client prefers this encoding`() {
        val app = ServerFilters.Compression(encoding, ContentEncoding.Gzip()).then { Response(OK).body("foobar") }

        val response = app(Request(GET, "").header("accept-encoding", "$name, gzip"))

        assertThat(response, hasHeader("content-encoding", name))
        assertThat(String(independentlyDecode(response.body.payload.array())), equalTo("foobar"))
    }

    @Test
    fun `decompresses request body`() {
        val app = ServerFilters.Compression(encoding).then { Response(OK).body(it.bodyString()) }

        assertThat(app(Request(POST, "").header("content-encoding", name).body(encoded("foobar"))), hasBody("foobar"))
    }

    @Test
    fun `does not encode an empty body`() {
        val app = ServerFilters.Compression(encoding).then { Response(OK) }

        assertThat(app(Request(GET, "").header("accept-encoding", name)), hasHeader("content-encoding", absent()))
    }

    @Test
    fun `rejects a body which inflates beyond the max size`() {
        val app = ServerFilters.Compression(encoding, maxDecompressedSize = 1000).then { Response(OK).body(it.bodyString()) }
        val bomb = encoded(String(ByteArray(10_000)))

        assertThat(app(Request(POST, "").header("content-encoding", name).body(bomb)), hasStatus(REQUEST_ENTITY_TOO_LARGE))
    }

    @Test
    fun `rejects a corrupt request body`() {
        val app = ServerFilters.Compression(encoding).then { Response(OK).body(it.bodyString()) }

        assertThat(
            app(Request(POST, "").header("content-encoding", name).body("this is not compressed")),
            hasStatus(BAD_REQUEST)
        )
    }

    @Test
    fun `client decompresses response`() {
        val server = ServerFilters.Compression(encoding).then { Response(OK).body("foobar") }

        assertThat(ClientFilters.Decompression(encoding).then(server)(Request(GET, "")), hasBody("foobar"))
    }

    @Test
    fun `client compresses request body`() {
        val client = ClientFilters.Compression(encoding).then {
            Response(OK).body(String(independentlyDecode(it.body.payload.array())))
        }

        assertThat(client(Request(POST, "").body("foobar")), hasBody("foobar"))
    }

    @Test
    fun `compresses a streamed response`() {
        val app = ServerFilters.Compression(encoding).then { Response(OK).body("foobar".byteInputStream()) }

        val response = app(Request(GET, "").header("accept-encoding", name))

        assertThat(String(independentlyDecode(response.body.stream.readBytes())), equalTo("foobar"))
    }

    private fun encoded(text: String) =
        Body(ByteBuffer.wrap(ByteArrayOutputStream().also { out -> encoding.encoder(out).use { it.write(text.toByteArray()) } }.toByteArray()))
}
