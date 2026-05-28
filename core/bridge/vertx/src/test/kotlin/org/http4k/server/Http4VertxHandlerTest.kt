package org.http4k.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import org.http4k.bridge.fallbackToHttp4k
import org.http4k.client.JavaHttpClient
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.net.Socket

class VertxToHttp4kHandlerTest : PortBasedTest {

    @Test
    fun `passes requests through and adapts`() {
        serving({ req -> Response(OK).headers(req.headers).body(req.body) }) { port ->
            val response = JavaHttpClient()(
                Request(POST, "http://localhost:$port").header("foo", "bar").body("hello")
            )

            assertThat(response, hasStatus(OK).and(hasBody("hello")).and(hasHeader("foo", "bar")))
        }
    }

    @Test
    fun `streams response body without realising the payload`() {
        serving({ Response(OK).body(StreamOnlyBody("hello".toByteArray())) }) { port ->
            val response = JavaHttpClient()(Request(POST, "http://localhost:$port"))

            assertThat(response, hasStatus(OK).and(hasBody("hello")))
        }
    }

    @Test
    fun `streams a large response body to the client`() {
        val large = "x".repeat(5_000_000)
        serving({ Response(OK).body(ByteArrayInputStream(large.toByteArray()), large.length.toLong()) }) { port ->
            val response = JavaHttpClient()(Request(POST, "http://localhost:$port"))

            assertThat(response, hasStatus(OK).and(hasBody(large)))
        }
    }

    @Test
    fun `streams a large request body to the handler without truncation`() {
        val large = "x".repeat(5_000_000)
        serving({ req -> Response(OK).body(req.body.stream.readBytes().size.toString()) }) { port ->
            val response = JavaHttpClient()(Request(POST, "http://localhost:$port").body(large))

            assertThat(response, hasStatus(OK).and(hasBody("5000000")))
        }
    }

    @Test
    fun `invokes the handler before the whole request body has arrived`() {
        serving({ req -> Response(OK).body("got:" + req.body.stream.read().toChar()) }) { port ->
            Socket("localhost", port).use { socket ->
                socket.soTimeout = 10_000
                socket.getOutputStream().apply {
                    write("POST / HTTP/1.1\r\nHost: localhost\r\nTransfer-Encoding: chunked\r\n\r\n".toByteArray())
                    write("1\r\nh\r\n".toByteArray()) // a single chunk - the terminating "0\r\n\r\n" is withheld
                    flush()
                }

                val statusLine = socket.getInputStream().bufferedReader().readLine()

                assertThat(statusLine ?: "", containsSubstring("200"))
            }
        }
    }

    private fun serving(handler: HttpHandler, block: (Int) -> Unit) {
        val vertx = Vertx.builder().build()
        val router = Router.router(vertx).apply { fallbackToHttp4k(handler) }
        val server: HttpServer = vertx.createHttpServer().requestHandler(router)
        server.listen(0).toCompletionStage().toCompletableFuture().get()
        try {
            block(server.actualPort())
        } finally {
            server.close()
            vertx.close()
        }
    }
}

/**
 * A Body that can only be consumed as a stream - realising the payload (i.e. buffering it all
 * into memory) blows up, proving the bridge streams rather than buffers.
 */
private class StreamOnlyBody(private val bytes: ByteArray) : Body {
    override val stream get() = ByteArrayInputStream(bytes)
    override val payload get() = error("payload realised - response body was buffered instead of streamed")
    override val length = bytes.size.toLong()
    override fun close() {}
}
