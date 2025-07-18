package org.http4k.client

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.anything
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Method.TRACE
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.CONNECTION_REFUSED
import org.http4k.core.Status.Companion.FOUND
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.Status.Companion.UNKNOWN_HOST
import org.http4k.core.StreamBody
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.http4k.core.queries
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.hamkrest.hasBody
import org.http4k.server.ServerConfig
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.util.Locale.getDefault
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

abstract class HttpClientContract(
    serverConfig: (Int, ServerConfig.StopMode) -> ServerConfig,
    val client: HttpHandler,
    private val timeoutClient: HttpHandler = client
) : AbstractHttpClientContract(serverConfig) {

    @Test
    open fun `can forward response body to another request`() {
        val response = client(Request(GET, "http://localhost:$port/stream"))
        val echoResponse = client(Request(POST, "http://localhost:$port/echo").body(response.body))
        assertThat(echoResponse.bodyString(), equalTo("stream"))
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
        val response = client(Request(GET, "http://localhost:$port/echo").query("name", "John Doe 12:34"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring("/echo?name=John+Doe+12:34"))
    }

    @Test
    fun `performs simple POST request`() {
        val response = client(Request(POST, "http://localhost:$port/echo").body("foobar"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring("foobar"))
    }

    @Test
    open fun `performs simple POST request - stream`() {
        val response = client(Request(POST, "http://localhost:$port/echo").body("foobar".byteInputStream(), 6))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring("foobar"))
    }

    @Test
    fun `performs simple DELETE request`() {

        val response = client(Request(DELETE, "http://localhost:$port/echo"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), containsSubstring("delete"))
    }

    @Test
    fun `does not follow redirects`() {
        val response = client(Request(GET, "http://localhost:$port/redirect"))

        assertThat(response.status, equalTo(FOUND))
        assertThat(response.header("location"), equalTo("/someUri"))
    }

    @Test
    fun `does not store cookies`() {
        client(Request(GET, "http://localhost:$port/cookies-set").query("name", "foo").query("value", "bar"))

        val response = client(Request(GET, "http://localhost:$port/cookies"))

        assertThat(response.status.successful, equalTo(true))
        assertThat(response.bodyString(), !containsSubstring("foo"))
    }

    @Test
    fun `filters enable cookies and redirects`() {
        val enhancedClient = ClientFilters.FollowRedirects().then(ClientFilters.Cookies()).then(client)

        val response = enhancedClient(Request(GET, "http://localhost:$port/cookies-set").query("name", "foo").query("value", "bar"))

        assertThat(response.status.successful, equalTo(true))
        assertThat(response.bodyString(), containsSubstring("foo"))
    }

    @Test
    fun `empty body`() {
        val response = client(Request(GET, "http://localhost:$port/empty"))
        assertThat(response.status.successful, equalTo(true))
        assertThat(response.bodyString(), equalTo(""))
    }

    @Test
    fun `redirection response`() {
        val response = ClientFilters.FollowRedirects()
            .then(client)(Request(GET, "http://localhost:$port/relative-redirect").query("times", "5"))
        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), anything)
    }

    @Test
    open fun `send binary data`() {
        val response = client(Request(POST, "http://localhost:$port/check-image").body(Body(ByteBuffer.wrap(testImageBytes()))))
        assertThat(response.bodyString(), equalTo(""))
        assertThat(response.status, equalTo(OK))
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    open fun `download binary data`() {
        val response = client(Request(GET, "http://localhost:$port/image"))
        assertThat(Base64.encode(response.body.payload.array()), equalTo(Base64.encode(testImageBytes())))
    }

    @Test
    open fun `socket timeouts are converted into 504`() {
        val response = timeoutClient(Request(GET, "http://localhost:$port/delay?millis=150"))

        assertThat(response.status, equalTo(CLIENT_TIMEOUT))
    }

    @Test
    open fun `connection refused are converted into 503`() {
        val response = client(Request(GET, "http://localhost:1"))

        assertThat(response.status, equalTo(CONNECTION_REFUSED))
    }

    @Test
    open fun `unknown host are converted into 503`() {
        val response = client(Request(GET, "http://foobar.bill"))

        assertThat(response.status, equalTo(UNKNOWN_HOST))
    }

    @Test
    fun `can retrieve body for different statuses`() {
        listOf(200, 301, 404, 500).forEach { statusCode ->
            val response = client(Request(GET, "http://localhost:$port/status").query("status", statusCode.toString()))
            assertThat(response.status, equalTo(Status(statusCode, "")))
            assertThat(response.bodyString(), equalTo("body for status $statusCode"))
        }
    }

    @Test
    open fun `handles response with custom status message`() {
        listOf(200, 301, 404, 500).forEach { statusCode ->
            val response = client(Request(GET, "http://localhost:$port/status").query("status", statusCode.toString()))
            response.use {
                assertThat(response.status.description, equalTo("Description for $statusCode"))
            }
        }
    }

    @Test
    fun `handles empty response body for different statuses`() {
        listOf(200, 301, 400, 404, 500).forEach { statusCode ->
            val response = client(Request(GET, "http://localhost:$port/status-no-body").query("status", statusCode.toString()))
            assertThat(response.status, equalTo(Status(statusCode, "")))
            assertThat(response.bodyString(), equalTo(""))
        }
    }

    @Test
    fun `requests have expected headers`() {
        fun checkNoBannedHeaders(m: Method, vararg banned: String) {
            val response = client(Request(m, "http://localhost:$port/headers"))
            val bannedHeaders = banned.intersect(response.bodyString().split(","))
            assertThat("$m contained banned headers $bannedHeaders", bannedHeaders.isEmpty(), equalTo(true))
            response.close()
        }
        checkNoBannedHeaders(GET, "Transfer-encoding")
        checkNoBannedHeaders(TRACE, "Transfer-encoding")
        checkNoBannedHeaders(OPTIONS, "Transfer-encoding")
        checkNoBannedHeaders(DELETE, "Transfer-encoding")
        checkNoBannedHeaders(POST)
        checkNoBannedHeaders(PUT)
    }

    @Test
    open fun `can send multiple headers with same name`() {
        val response = client(Request(POST, "http://localhost:$port/multiRequestHeader").header("echo", "foo").header("echo", "bar"))

        assertThat(response, hasBody("echo: bar\necho: foo"))
    }

    @Test
    open fun `can receive multiple headers with same name`() {
        val response = client(Request(POST, "http://localhost:$port/multiResponseHeader"))

        assertThat(response.headerValues("serverHeader").toSet(), equalTo(setOf("foo", "bar")))
    }

    @Test
    open fun `can send multiple cookies`() {
        val response = client(Request(POST, "http://localhost:$port/multiRequestCookies").cookie(Cookie("foo", "vfoo")).cookie(Cookie("bar", "vbar")))

        assertThat(response, hasBody("bar: vbar\nfoo: vfoo"))
    }

    @Test
    open fun `can receive multiple cookies`() {
        val response = client(Request(POST, "http://localhost:$port/multiResponseCookies"))

        assertThat(response.cookies().sortedBy(Cookie::name).joinToString("\n") { "${it.name}: ${it.value}" }, equalTo("bar: vbar\nfoo: vfoo"))
    }

    @Test
    open fun `unhandled exceptions converted into 500`() {
        val response = client(Request(GET, "http://localhost:$port/boom"))

        assertThat(response.status, equalTo(INTERNAL_SERVER_ERROR))
    }

    @Test
    open fun `unknown host is correctly reported`() {
        val response = client(Request(GET, "http://reallynotarealserver.bob"))

        assertThat(response.status.code, equalTo(SERVICE_UNAVAILABLE.code))
        assertThat(response.status.toString().lowercase(getDefault()), containsSubstring("unknown"))
    }

    @Test
    open fun `fails with no protocol`() {
        assertThat(
            { client(Request(GET, "/boom").header("host", "localhost:$port")) }, throws<Exception>()
        )
    }

    @Test
    fun `host header not abusable`() {
        val response = client(Request(GET, "http://localhost:$port/hostheaders").header("host", "foobar:$port"))
        assertThat(response.bodyString(), !containsSubstring("foobar").and(containsSubstring(",")))
    }

    @Test
    open fun `supports query parameter list`() {
        val response = client(Request(GET, "http://localhost:$port/echo").query("p1", "foo").query("p1", "bar"))
        val uriFromResponse = Uri.of(response.bodyString())
        assertThat(uriFromResponse.queries(), equalTo(listOf("p1" to "foo", "p1" to "bar")))
    }

    @Test
    open fun `supports zero content-length`() {
        val response = client(Request(PUT, "http://localhost:$port/headerValues").header("content-length", "0"))
        assertThat(response.bodyString().lowercase(), containsSubstring("content-length=0"))
    }

    @Test
    open fun `supports explicit content-length`() {
        val response = client(Request(PUT, "http://localhost:$port/headerValues")
            .header("content-length", "3")
            .body("foo"))
        assertThat(response.bodyString().lowercase(), containsSubstring("content-length=3"))
    }

    @Test
    open fun `supports content-length set in body`() {
        val response = client(Request(PUT, "http://localhost:$port/headerValues")
            .body(StreamBody("foo".byteInputStream(), 3)))
        assertThat(response.bodyString().lowercase(), containsSubstring("content-length=3"))
    }

    @Test
    open fun `includes content-length for regular requests`() {
        val response = client(Request(PUT, "http://localhost:$port/headerValues").body("foo"))
        assertThat(response.bodyString().lowercase(), containsSubstring("content-length=3"))
    }

    @Test
    open fun `keeps the csv header value as it is`() {
        val response = client(Request(PUT, "http://localhost:$port/csvHeader"))
        val values = response.headerValues("foo")
        assertThat(values, equalTo(listOf("bar=baz,toll=troll")))
    }

    @Test
    @Disabled
    fun `sanitises uri`() {
        val response = client(Request(GET, "http://localhost:$port/encoded-uri/foo, bar & baz!"))
        assertThat(response.bodyString(), equalTo("foo, bar & baz!"))
    }
}
