package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
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
            else -> Response(Status.OK).let { if (request.query("foo") != null) it.body("with query") else it }
        }
    }

    val followRedirects = ClientFilters.FollowRedirects().then(server)

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
        assertThat(svc(Request(GET, "")), equalTo(Response(OK)))
        assertThat(start, equalTo(Request(GET, "") to zipkinTraces))
        assertThat(end, equalTo(Triple(Request(GET, ""), Response(OK), zipkinTraces)))
    }

    @Test
    fun `adds new request tracing to outgoing request when not present`() {
        val svc = ClientFilters.RequestTracing().then { it ->
            assertThat(ZipkinTraces(it), present())
            Response(OK)
        }
        assertThat(svc(Request(GET, "")), equalTo(Response(OK)))
    }

    @Test
    fun `set host on client`() {
        val handler = ClientFilters.SetHostFrom(Uri.of("http://localhost:8080")).then { Response(OK).body(it.uri.toString()) }
        assertThat(handler(Request(GET, "/loop")).bodyString(), equalTo("http://localhost:8080/loop"))
    }

}