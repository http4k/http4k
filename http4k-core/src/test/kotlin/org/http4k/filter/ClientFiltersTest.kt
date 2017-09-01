package org.http4k.filter

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.toBody
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ClientFiltersTest {
    val server = { request: Request ->
        when (request.uri.path) {
            "/redirect" -> Response(Status.FOUND).header("location", "/ok")
            "/loop" -> Response(Status.FOUND).header("location", "/loop")
            "/absolute-target" -> if (request.uri.host == "example.com") Response(Status.OK).body("absolute") else Response(Status.INTERNAL_SERVER_ERROR)
            "/absolute-redirect" -> Response(Status.MOVED_PERMANENTLY).header("location", "http://example.com/absolute-target")
            "/redirect-with-charset" -> Response(Status.MOVED_PERMANENTLY).header("location", "/destination; charset=utf8")
            "/destination" -> Response(OK).body("destination")
            else -> Response(Status.OK).let { if (request.query("foo") != null) it.body("with query") else it }
        }
    }

    private val followRedirects = ClientFilters.FollowRedirects().then(server)

    @Test
    fun `does not follow redirect by default`() {
        val defaultClient = server
        assertThat(defaultClient(Request(GET, "/redirect")), equalTo(Response(Status.FOUND).header("location", "/ok")))
    }

    @Test
    fun `follows redirect for temporary redirect response`() {
        assertThat(followRedirects(Request(GET, "/redirect")), equalTo(Response(Status.OK)))
    }

    @Test
    fun `does not follow redirect for post`() {
        assertThat(followRedirects(Request(POST, "/redirect")), equalTo(Response(Status.FOUND).header("location", "/ok")))
    }

    @Test
    fun `does not follow redirect for put`() {
        assertThat(followRedirects(Request(PUT, "/redirect")), equalTo(Response(Status.FOUND).header("location", "/ok")))
    }

    @Test
    fun `supports absolute redirects`() {
        assertThat(followRedirects(Request(GET, "/absolute-redirect")), equalTo(Response(Status.OK).body("absolute")))
    }

    @Test
    fun `discards query parameters in relative redirects`() {
        assertThat(followRedirects(Request(GET, "/redirect?foo=bar")), equalTo(Response(Status.OK)))
    }

    @Test
    fun `discards charset from location header`() {
        assertThat(followRedirects(Request(GET, "/redirect-with-charset")), equalTo(Response(Status.OK).body("destination")))
    }

    @Test
    fun `prevents redirection loop after 10 redirects`() {
        try {
            followRedirects(Request(GET, "/loop"))
            fail("should have looped")
        } catch (e: IllegalStateException) {
            assertThat(e.message, equalTo("Too many redirection"))
        }
    }

    @Before
    fun before() {
        ZipkinTraces.THREAD_LOCAL.remove()
    }

    @Test
    fun `adds request tracing to outgoing request when already present`() {
        val zipkinTraces = ZipkinTraces(TraceId.new(), TraceId.new(), TraceId.new())
        ZipkinTraces.THREAD_LOCAL.set(zipkinTraces)

        var start: Pair<Request, ZipkinTraces>? = null
        var end: Triple<Request, Response, ZipkinTraces>? = null

        val svc = ClientFilters.RequestTracing(
            { req, trace -> start = req to trace },
            { req, resp, trace -> end = Triple(req, resp, trace) }
        ).then { it ->
            assertThat(ZipkinTraces(it), equalTo(zipkinTraces))
            Response(OK)
        }

        svc(Request(GET, "")) shouldMatch equalTo(Response(OK))
        assertThat(start, equalTo(Request(GET, "") to zipkinTraces))
        assertThat(end, equalTo(Triple(Request(GET, ""), Response(OK), zipkinTraces)))
    }

    @Test
    fun `adds new request tracing to outgoing request when not present`() {
        val svc = ClientFilters.RequestTracing().then { it ->
            assertThat(ZipkinTraces(it), present())
            Response(OK)
        }

        svc(Request(GET, "")) shouldMatch equalTo(Response(OK))
    }

    @Test
    fun `set host on client`() {
        val handler = ClientFilters.SetHostFrom(Uri.of("http://localhost:8080")).then { Response(OK).header("Host", it.header("Host")).body(it.uri.toString()) }
        handler(Request(GET, "/loop")) shouldMatch hasBody("http://localhost:8080/loop").and(hasHeader("Host", "localhost"))
    }

    @Test
    fun `gzip request and gunzip response`() {
        val handler = ClientFilters.GZip().then {
            it shouldMatch hasHeader("transfer-encoding", "gzip").and(hasBody(equalTo("hello".toBody().gzipped())))
            Response(OK).header("transfer-encoding", "gzip").body(it.body)
        }

        handler(Request(GET, "/").body("hello")) shouldMatch hasBody("hello")
    }

    @Test
    fun `passes through non-gzipped response`() {
        val handler = ClientFilters.GZip().then {
            Response(OK).body("hello")
        }

        handler(Request(GET, "/").body("hello")) shouldMatch hasBody("hello")
    }
}