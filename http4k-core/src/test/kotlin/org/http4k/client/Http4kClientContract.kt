package org.http4k.client

import com.natpryce.hamkrest.anything
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.util.RetryRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.nio.ByteBuffer
import java.util.*

abstract class Http4kClientContract(private val serverConfig: (Int) -> ServerConfig,
                                    val client: HttpHandler,
                                    private val timeoutClient: HttpHandler) {
    @Rule
    @JvmField
    var retryRule = RetryRule(5)

    private var server: Http4kServer? = null

    val port = Random().nextInt(1000) + 8000

    @Before
    fun before() {
        val defaultHandler = { request: Request ->
            Response(OK)
                    .header("uri", request.uri.toString())
                    .header("header", request.header("header"))
                    .header("query", request.query("query"))
                    .body(request.body)
        }
        server = routes("/someUri" bind POST to defaultHandler,
            "/empty" bind GET to { _: Request -> Response(OK).body("") },
            "/redirect" bind GET to { _: Request -> Response(FOUND).header("Location", "/someUri").body("") },
            "/stream" bind GET to { _: Request -> Response(OK).body("stream".byteInputStream()) },
            "/echo" bind POST to { request: Request -> Response(OK).body(request.bodyString()) },
            "/check-image" bind POST to { request: Request ->
                if (Arrays.equals(testImageBytes(), request.body.payload.array()))
                    Response(OK) else Response(BAD_REQUEST.description("Image content does not match"))
            })
            .asServer(serverConfig(port)).start()
    }

    @Test
    fun `can forward response body to another request`() {
        val response = client(Request(GET, "http://localhost:$port/stream"))
        val echoResponse = client(Request(POST, "http://localhost:$port/echo").body(response.body))
        echoResponse.bodyString().shouldMatch(equalTo("stream"))
    }

    @Test
    fun `supports gzipped content`() {
        val asServer = ServerFilters.GZip().then { Response(Status.OK).body("hello") }.asServer(SunHttp())
        asServer.start()
        val client = ApacheClient()

        val request = Request(Method.GET, "http://localhost:8000").header("accept-encoding", "gzip")
        client(request)
        client(request)
        client(request)
        asServer.stop()
    }

    @After
    fun after() {
        server?.stop()
    }

    @Test
    fun `can make call`() {
        val response = client(Request(POST, "http://localhost:$port/someUri")
                .query("query", "123")
                .header("header", "value").body("body"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.header("uri"), equalTo("/someUri?query=123"))
        assertThat(response.header("query"), equalTo("123"))
        assertThat(response.header("header"), equalTo("value"))
        assertThat(response.bodyString(), equalTo("body"))
    }

    @Test
    fun `performs simple GET request`() {
        val response = client(Request(GET, "http://httpbin.org/get").query("name", "John Doe"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring("John Doe"))
    }

    @Test
    fun `performs simple POST request`() {
        val response = client(Request(POST, "http://httpbin.org/post"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring(""))
    }

    @Test
    fun `performs simple DELETE request`() {
        val response = client(Request(DELETE, "http://httpbin.org/delete"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring(""))
    }

    @Test
    fun `does not follow redirects`() {
        val response = client(Request(GET, "http://httpbin.org/redirect-to").query("url", "/destination"))

        assertThat(response.status, equalTo(Status.FOUND))
        assertThat(response.header("location"), equalTo("/destination"))
    }

    @Test
    fun `does not store cookies`() {
        client(Request(GET, "http://httpbin.org/cookies/set").query("foo", "bar"))

        val response = client(Request(GET, "http://httpbin.org/cookies"))

        assertThat(response.status.successful, equalTo(true))
        assertThat(response.bodyString(), !containsSubstring("foo"))
    }

    @Test
    fun `filters enable cookies and redirects`() {
        val enhancedClient = ClientFilters.FollowRedirects().then(ClientFilters.Cookies()).then(client)

        val response = enhancedClient(Request(GET, "http://httpbin.org/cookies/set").query("foo", "bar"))

        assertThat(response.status.successful, equalTo(true))
        assertThat(response.bodyString(), containsSubstring("foo"))
    }

    @Test
    fun `empty body`() {
        val response = client(Request(Method.GET, "http://localhost:$port/empty"))
        response.status.successful.shouldMatch(equalTo(true))
        response.bodyString().shouldMatch(equalTo(""))
    }

    @Test
    fun `redirection response`() {
        val response = ClientFilters.FollowRedirects()
            .then(client)(Request(Method.GET, "http://httpbin.org/relative-redirect/5"))
        response.status.shouldMatch(equalTo(OK))
        response.bodyString().shouldMatch(anything)
    }

    @Test
    fun `send binary data`() {
        val response = client(Request(Method.POST, "http://localhost:$port/check-image").body(Body(ByteBuffer.wrap(testImageBytes()))))
        response.status.shouldMatch(equalTo(OK))
    }

    @Test
    fun `socket timeouts are converted into 504`() {
        val response = timeoutClient(Request(GET, "http://httpbin.org/delay/2"))

        assertThat(response.status, equalTo(Status.CLIENT_TIMEOUT))
    }

    private fun testImageBytes() = this::class.java.getResourceAsStream("/test.png").readBytes()
}