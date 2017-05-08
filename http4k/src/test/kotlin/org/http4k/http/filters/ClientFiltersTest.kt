package org.http4k.http.filters

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.http.core.Request
import org.http4k.http.core.Request.Companion.get
import org.http4k.http.core.Request.Companion.post
import org.http4k.http.core.Request.Companion.put
import org.http4k.http.core.Response
import org.http4k.http.core.Response.Companion.movedPermanently
import org.http4k.http.core.Response.Companion.movedTemporarily
import org.http4k.http.core.Response.Companion.ok
import org.http4k.http.core.Response.Companion.serverError
import org.http4k.http.core.Status.Companion.OK
import org.http4k.http.core.then
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ClientFiltersTest {
    val server = { request: Request ->
        when (request.uri.path) {
            "/redirect" -> movedTemporarily(listOf("location" to "/ok"))
            "/loop" -> movedTemporarily(listOf("location" to "/loop"))
            "/absolute-target" -> if (request.uri.host == "example.com") ok().body("absolute") else serverError()
            "/absolute-redirect" -> movedPermanently(listOf("location" to "http://example.com/absolute-target"))
            else -> ok().let { if (request.query("foo") != null) it.body("with query") else it }
        }
    }

    val followRedirects = ClientFilters.FollowRedirects().then(server)

    @Test
    fun `does not follow redirect by default`() {
        val defaultClient = server
        assertThat(defaultClient(get("/redirect")), equalTo(movedTemporarily(listOf("location" to "/ok"))))
    }

    @Test
    fun `follows redirect for temporary redirect response`() {
        assertThat(followRedirects(get("/redirect")), equalTo(ok()))
    }

    @Test
    fun `does not follow redirect for post`() {
        assertThat(followRedirects(post("/redirect")), equalTo(movedTemporarily(listOf("location" to "/ok"))))
    }

    @Test
    fun `does not follow redirect for put`() {
        assertThat(followRedirects(put("/redirect")), equalTo(movedTemporarily(listOf("location" to "/ok"))))
    }

    @Test
    fun `supports absolute redirects`() {
        assertThat(followRedirects(get("/absolute-redirect")), equalTo(ok().body("absolute")))
    }

    @Test
    fun `discards query parameters in relative redirects`() {
        assertThat(followRedirects(get("/redirect?foo=bar")), equalTo(ok()))
    }

    @Test
    fun `prevents redirection loop after 10 redirects`() {
        try {
            followRedirects(get("/loop"))
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
        assertThat(svc(get("")), equalTo(Response(OK)))
        assertThat(start, equalTo(get("") to zipkinTraces))
        assertThat(end, equalTo(Triple(get(""), Response(OK), zipkinTraces)))
    }

    @Test
    fun `adds new request tracing to outgoing request when not present`() {
        val svc = ClientFilters.RequestTracing().then { it ->
            assertThat(ZipkinTraces(it), present())
            Response(OK)
        }
        assertThat(svc(get("")), equalTo(Response(OK)))
    }
}