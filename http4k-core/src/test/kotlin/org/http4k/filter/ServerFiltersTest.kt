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
import org.http4k.hamkrest.hasStatus
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
        ZipkinTraces(actual) shouldMatch equalTo(originalTraces)

        start!!.first shouldMatch equalTo(originalRequest)
        start!!.second shouldMatch equalTo(originalTraces)


        end!!.first shouldMatch equalTo(originalRequest)
        end!!.second shouldMatch equalTo(ZipkinTraces(originalTraces, Response(OK)))
        end!!.third shouldMatch equalTo(originalTraces)
    }

    @Test
    fun `GET - Cors headers are set correctly`() {
        val handler = ServerFilters.Cors(UnsafeGlobalPermissive).then { Response(I_M_A_TEAPOT) }
        val response = handler(Request(GET, "/"))

        response shouldMatch hasStatus(I_M_A_TEAPOT)
            .and(hasHeader("access-control-allow-origin", "*"))
            .and(hasHeader("access-control-allow-headers", "content-type"))
            .and(hasHeader("access-control-allow-methods", "GET, POST, PUT, DELETE, OPTIONS, TRACE, PATCH, PURGE, HEAD"))
    }

    @Test
    fun `OPTIONS - requests are intercepted and returned with expected headers`() {
        val handler = ServerFilters.Cors(CorsPolicy(listOf("foo", "bar"), listOf("rita", "sue", "bob"), listOf(DELETE, POST))).then { Response(INTERNAL_SERVER_ERROR) }
        val response = handler(Request(OPTIONS, "/"))

        response shouldMatch hasStatus(OK)
            .and(hasHeader("access-control-allow-origin", "foo, bar"))
            .and(hasHeader("access-control-allow-headers", "rita, sue, bob"))
            .and(hasHeader("access-control-allow-methods", "DELETE, POST"))
    }

    @Test
    fun `catch all exceptions`() {
        val e = RuntimeException("boom!")
        val handler = ServerFilters.CatchAll(I_M_A_TEAPOT).then { throw e }

        val response = handler(Request(GET, "/").header("foo", "one").header("bar", "two"))

        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))

        response shouldMatch hasStatus(I_M_A_TEAPOT).and(hasBody(sw.toString()))
    }

    @Test
    fun `copy headers from request to response`() {
        val handler = ServerFilters.CopyHeaders("foo", "bar").then { Response(OK) }

        val response = handler(Request(GET, "/").header("foo", "one").header("bar", "two"))

        response shouldMatch hasHeader("foo", "one")
        response shouldMatch hasHeader("bar", "two")
    }

    @Test
    fun `copy only headers specified in filter`() {
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

        handler(Request(GET, "/").header("accept-encoding", "gzip").header("transfer-encoding", "gzip").body("hello".toBody().gzipped())) shouldMatch
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
