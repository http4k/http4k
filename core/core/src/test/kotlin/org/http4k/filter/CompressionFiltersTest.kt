package org.http4k.filter

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Body
import org.http4k.core.ContentEncoding
import org.http4k.core.ContentEncodingName
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Gzip
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.REQUEST_ENTITY_TOO_LARGE
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream

class CompressionFiltersTest {

    private val gzip = ContentEncoding.Gzip()

    private val reverse = ContentEncoding(
        ContentEncodingName("x-reverse"),
        { out ->
            object : ByteArrayOutputStream() {
                override fun close() = out.run { write(toByteArray().reversedArray()); close() }
            }
        },
        { ByteArrayInputStream(it.readBytes().reversedArray()) }
    )

    private fun compressing(vararg encodings: ContentEncoding) =
        ResponseFilters.Compression(*encodings).then { Response(OK).body("foobar") }

    @Test
    fun `compresses response with gzip when client accepts it`() {
        assertThat(
            compressing(gzip)(Request(GET, "").header("accept-encoding", "gzip")),
            hasBody(equalTo<Body>(Body("foobar").gzipped().body))
                .and(hasHeader("content-encoding", "gzip"))
                .and(hasHeader("vary", "accept-encoding"))
        )
    }

    @Test
    fun `leaves response alone when client accepts none of the encodings`() {
        assertThat(
            compressing(gzip)(Request(GET, "").header("accept-encoding", "deflate")),
            hasBody("foobar").and(hasHeader("content-encoding", absent())).and(hasHeader("vary", absent()))
        )
    }

    @Test
    fun `leaves response alone when client sends no accept-encoding`() {
        assertThat(compressing(gzip)(Request(GET, "")), hasBody("foobar").and(hasHeader("content-encoding", absent())))
    }

    @Test
    fun `prefers server order when client has no preference`() {
        assertThat(
            compressing(reverse, gzip)(Request(GET, "").header("accept-encoding", "gzip, x-reverse")),
            hasBody("raboof").and(hasHeader("content-encoding", "x-reverse"))
        )
    }

    @Test
    fun `prefers the encoding with the highest client quality`() {
        assertThat(
            compressing(reverse, gzip)(Request(GET, "").header("accept-encoding", "x-reverse;q=0.5, gzip;q=0.8")),
            hasHeader("content-encoding", "gzip")
        )
    }

    @Test
    fun `never uses an encoding the client rejects with zero quality`() {
        assertThat(
            compressing(gzip)(Request(GET, "").header("accept-encoding", "gzip;q=0")),
            hasBody("foobar").and(hasHeader("content-encoding", absent()))
        )
    }

    @Test
    fun `wildcard accepts encodings not otherwise listed`() {
        assertThat(
            compressing(reverse, gzip)(Request(GET, "").header("accept-encoding", "x-reverse;q=0, *")),
            hasHeader("content-encoding", "gzip")
        )
    }

    @Test
    fun `matches encoding names case-insensitively`() {
        assertThat(
            compressing(gzip)(Request(GET, "").header("accept-encoding", "GZIP")),
            hasHeader("content-encoding", "gzip")
        )
    }

    @Test
    fun `leaves response alone when accept-encoding is malformed`() {
        assertThat(
            compressing(gzip)(Request(GET, "").header("accept-encoding", "gzip;q=banana")),
            hasBody("foobar").and(hasHeader("content-encoding", absent()))
        )
    }

    @Test
    fun `does not encode a response which is already encoded`() {
        val app = ResponseFilters.Compression(gzip).then {
            Response(OK).header("content-encoding", "x-reverse").body("raboof")
        }
        assertThat(
            app(Request(GET, "").header("accept-encoding", "gzip")),
            hasBody("raboof").and(hasHeader("content-encoding", "x-reverse"))
        )
    }

    private val echo = { req: Request -> Response(OK).body(req.bodyString()) }

    @Test
    fun `decompresses request body with a matching content-encoding`() {
        val app = RequestFilters.Decompression(reverse, gzip).then(echo)
        assertThat(app(Request(POST, "").header("content-encoding", "x-reverse").body("raboof")), hasBody("foobar"))
    }

    @Test
    fun `leaves request body alone when content-encoding is not one we support`() {
        val app = RequestFilters.Decompression(gzip).then(echo)
        assertThat(app(Request(POST, "").header("content-encoding", "x-reverse").body("raboof")), hasBody("raboof"))
    }

    @Test
    fun `rejects request body which inflates beyond the max size`() {
        val app = RequestFilters.Decompression(gzip, maxDecompressedSize = 3).then(echo)
        assertThat(
            app(Request(POST, "").header("content-encoding", "gzip").body(Body("foobar").gzipped().body)),
            hasStatus(REQUEST_ENTITY_TOO_LARGE)
        )
    }

    @Test
    fun `rejects a corrupt request body`() {
        val app = RequestFilters.Decompression(gzip).then(echo)
        assertThat(
            app(Request(POST, "").header("content-encoding", "gzip").body("this is not gzip")),
            hasStatus(BAD_REQUEST)
        )
    }

    @Test
    fun `does not mask failures in the handler as a bad request`() {
        val app = RequestFilters.Decompression(gzip).then { it.bodyString(); throw IOException("downstream") }
        assertThat(
            { app(Request(POST, "").header("content-encoding", "gzip").body(Body("foobar").gzipped().body)) },
            throws<IOException>()
        )
    }

    @Test
    fun `server compression decompresses requests and compresses responses`() {
        val app = ServerFilters.Compression(reverse, gzip).then(echo)
        assertThat(
            app(
                Request(POST, "")
                    .header("content-encoding", "gzip")
                    .header("accept-encoding", "x-reverse")
                    .body(Body("foobar").gzipped().body)
            ),
            hasBody("raboof").and(hasHeader("content-encoding", "x-reverse"))
        )
    }

    @Test
    fun `decompressed request no longer claims to be encoded`() {
        val app = RequestFilters.Decompression(reverse).then { Response(OK).body(it.header("content-encoding") ?: "none") }
        assertThat(app(Request(POST, "").header("content-encoding", "x-reverse").body("raboof")), hasBody("none"))
    }

    @Test
    fun `client asks for the encodings it can decode, in order`() {
        val client = ResponseFilters.Decompression(reverse, gzip).then { Response(OK).body(it.header("accept-encoding")!!) }
        assertThat(client(Request(GET, "")), hasBody("x-reverse,gzip"))
    }

    @Test
    fun `client decompresses response with a matching content-encoding`() {
        val client = ResponseFilters.Decompression(reverse, gzip).then {
            Response(OK).header("content-encoding", "x-reverse").header("content-length", "6").body("raboof")
        }
        assertThat(
            client(Request(GET, "")),
            hasBody("foobar").and(hasHeader("content-encoding", absent())).and(hasHeader("content-length", absent()))
        )
    }

    @Test
    fun `client leaves response alone when content-encoding is not one it supports`() {
        val client = ResponseFilters.Decompression(gzip).then { Response(OK).header("content-encoding", "x-reverse").body("raboof") }
        assertThat(client(Request(GET, "")), hasBody("raboof").and(hasHeader("content-encoding", "x-reverse")))
    }

    @Test
    fun `client compresses request body with the given encoding`() {
        val client = RequestFilters.Compression(reverse).then { Response(OK).body(it.header("content-encoding") + ":" + it.bodyString()) }
        assertThat(client(Request(POST, "").body("foobar")), hasBody("x-reverse:raboof"))
    }

    @Test
    fun `client compression corrects the request content-length`() {
        val client = RequestFilters.Compression(gzip).then { Response(OK).body(it.header("content-length") ?: "none") }
        val compressedLength = Body("foobar").gzipped().body.length.toString()

        assertThat(client(Request(POST, "").header("content-length", "6").body("foobar")), hasBody(compressedLength))
    }

    @Test
    fun `client does not encode an empty request body`() {
        val client = RequestFilters.Compression(reverse).then { Response(OK).body(it.header("content-encoding") ?: "none") }
        assertThat(client(Request(POST, "")), hasBody("none"))
    }

    @Test
    fun `client and server compression interoperate`() {
        val server = ServerFilters.Compression(reverse, gzip).then(echo)
        val onTheWire = mutableListOf<String?>()
        val client = ClientFilters.Compression(gzip, reverse).then { request ->
            onTheWire += request.header("content-encoding")
            server(request).also { onTheWire += it.header("content-encoding") }
        }

        assertThat(client(Request(POST, "").body("foobar")), hasBody("foobar"))
        assertThat(onTheWire, equalTo(listOf<String?>("gzip", "x-reverse")))
    }

    @Test
    fun `does not compress a response the guard rejects`() {
        val app = ResponseFilters.Compression(gzip, shouldCompress = { false }).then { Response(OK).body("foobar") }
        assertThat(
            app(Request(GET, "").header("accept-encoding", "gzip")),
            hasBody("foobar").and(hasHeader("content-encoding", absent()))
        )
    }

    @Test
    fun `server compression only compresses compressible content types`() {
        val app = ServerFilters.Compression(gzip, shouldCompress = compressibleContentTypes(TEXT_HTML)).then {
            Response(OK).header("content-type", it.uri.path).body("foobar")
        }
        val request = Request(GET, "").header("accept-encoding", "gzip")

        assertThat(app(request.uri(Uri.of("text/html; charset=utf-8"))), hasHeader("content-encoding", "gzip"))
        assertThat(app(request.uri(Uri.of("image/png"))), hasHeader("content-encoding", absent()))
    }

    @Test
    fun `compresses a streamed response as it is read, without buffering it`() {
        var sourceRead = false
        val source = object : InputStream() {
            val bytes = ByteArrayInputStream("foobar".toByteArray())
            override fun read() = bytes.read().also { sourceRead = true }
        }
        val app = ResponseFilters.Compression(gzip).then {
            Response(OK).header("content-length", "6").body(source)
        }

        val response = app(Request(GET, "").header("accept-encoding", "gzip"))

        assertThat(sourceRead, equalTo(false))
        assertThat(response, hasHeader("content-length", absent()))
        assertThat(String(GZIPInputStream(response.body.stream).readBytes()), equalTo("foobar"))
    }
}
