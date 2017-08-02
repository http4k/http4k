package org.http4k.filter

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Headers
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.toBody
import org.http4k.filter.CorsPolicy.Companion.UnsafeGlobalPermissive
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.junit.Before
import org.junit.Test
import java.io.PrintWriter
import java.io.StringWriter

class ServerFiltersTest {

    @Before
    fun before() {
        ZipkinTraces.THREAD_LOCAL.remove()
    }

    @Test
    fun `initialises request tracing on request and sets on outgoing response when not present`() {
        var newThreadLocal: ZipkinTraces? = null
        val svc = ServerFilters.RequestTracing().then {
            newThreadLocal = ZipkinTraces.THREAD_LOCAL.get()!!
            newThreadLocal!!.spanId shouldMatch present()
            val setOnRequest = ZipkinTraces(it)
            setOnRequest.traceId shouldMatch equalTo(newThreadLocal!!.traceId)
            setOnRequest.spanId shouldMatch equalTo(newThreadLocal!!.parentSpanId)
            Response(OK)
        }

        val received = ZipkinTraces(svc(Request(GET, "")))

        received shouldMatch equalTo(ZipkinTraces(newThreadLocal!!.traceId, newThreadLocal!!.parentSpanId!!, null))
    }

    @Test
    fun `uses existing request tracing from request and sets on outgoing response not present`() {
        val originalTraceId = TraceId("originalTrace")
        val originalSpanId = TraceId("originalSpan")
        val originalParentSpanId = TraceId("originalParentSpanId")
        val originalTraces = ZipkinTraces(originalTraceId, originalSpanId, originalParentSpanId)

        var start: Pair<Request, ZipkinTraces>? = null
        var end: Triple<Request, Response, ZipkinTraces>? = null

        val svc = ServerFilters.RequestTracing(
            { req, trace -> start = req to trace },
            { req, resp, trace -> end = Triple(req, resp, trace) }
        ).then {
            val actual = ZipkinTraces.THREAD_LOCAL.get()
            val setOnRequest = ZipkinTraces(it)
            setOnRequest.traceId shouldMatch equalTo(actual.traceId)
            setOnRequest.spanId shouldMatch equalTo(actual.parentSpanId)

            actual.traceId shouldMatch equalTo(originalTraceId)
            actual.spanId shouldMatch present()
            actual.parentSpanId shouldMatch equalTo(originalSpanId)
            Response(OK)
        }

        val originalRequest = ZipkinTraces(originalTraces, Request(GET, ""))
        val actual = svc(originalRequest)
        assertThat(ZipkinTraces(actual), equalTo(originalTraces))

        assertThat(start!!.first, equalTo(originalRequest))
        assertThat(start!!.second, equalTo(originalTraces))

        assertThat(end!!.first, equalTo(originalRequest))
        assertThat(end!!.second, equalTo(ZipkinTraces(originalTraces, Response(OK))))
        assertThat(end!!.third, equalTo(originalTraces))
    }

    @Test
    fun `GET - Cors headers are set correctly`() {
        val handler = ServerFilters.Cors(UnsafeGlobalPermissive).then { Response(I_M_A_TEAPOT)}
        val response = handler(Request(GET, "/"))
        assertThat(response.status, equalTo(I_M_A_TEAPOT))
        assertThat(response.header("access-control-allow-origin"), equalTo("*"))
        assertThat(response.header("access-control-allow-headers"), equalTo("content-type"))
        assertThat(response.header("access-control-allow-methods"), equalTo("GET, POST, PUT, DELETE, OPTIONS, TRACE, PATCH, PURGE"))
    }

    @Test
    fun `OPTIONS - requests are intercepted and returned with expected headers`() {
        val handler = ServerFilters.Cors(CorsPolicy(listOf("foo", "bar"), listOf("rita", "sue", "bob"), listOf(DELETE, POST))).then { Response(INTERNAL_SERVER_ERROR)}
        val response = handler(Request(OPTIONS, "/"))
        assertThat(response.status, equalTo(OK))
        assertThat(response.header("access-control-allow-origin"), equalTo("foo, bar"))
        assertThat(response.header("access-control-allow-headers"), equalTo("rita, sue, bob"))
        assertThat(response.header("access-control-allow-methods"), equalTo("DELETE, POST"))
    }

    @Test
    fun `catch all exceptions`() {
        val e = RuntimeException("boom!")
        val handler = ServerFilters.CatchAll(I_M_A_TEAPOT).then { throw e }

        val response = handler(Request(GET, "/").header("foo", "one").header("bar", "two"))

        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))

        assertThat(response.status, equalTo(I_M_A_TEAPOT))
        assertThat(response.bodyString(), equalTo(sw.toString()))
    }

    @Test
    fun `copy headers from request to response`() {
        val handler = ServerFilters.CopyHeaders("foo", "bar").then { Response(OK) }

        val response = handler(Request(GET, "/").header("foo", "one").header("bar", "two"))

        assertThat(response.header("foo"), equalTo("one"))
        assertThat(response.header("bar"), equalTo("two"))
    }

    @Test
    fun `copy only headers specified in filter`(){
        val handler = ServerFilters.CopyHeaders("a", "b").then { Response(OK) }

        val response = handler(Request(GET, "/").header("b", "2").header("c", "3"))

        assertThat(response.headers, equalTo(listOf("b" to "2") as Headers))
    }

    @Test
    fun `gunzip request and gzip response`() {
        val handler = ServerFilters.GZip().then {
            it shouldMatch hasBody(equalTo("hello"))
            Response(OK).body(it.body)
        }

        handler(Request(GET, "/").header("transfer-encoding", "gzip").body("hello".toBody().gzipped())) shouldMatch
            hasHeader("transfer-encoding", "gzip").and(hasBody(equalTo("hello".toBody().gzipped())))
    }

    @Test
    fun `passes through non-gzipped request`() {
        val handler = ServerFilters.GZip().then {
            it shouldMatch hasBody("hello")
            Response(OK).body("hello")
        }

        handler(Request(GET, "/").body("hello"))
    }
}
