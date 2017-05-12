package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.junit.Before
import org.junit.Test

class ServerFiltersTest {

    @Before
    fun before() {
        ZipkinTraces.THREAD_LOCAL.remove()
    }

    @Test
    fun `initialises request tracing and sets on outgoing response when not present`() {
        var newThreadLocal: ZipkinTraces? = null
        val svc = ServerFilters.RequestTracing().then {
            newThreadLocal = ZipkinTraces.THREAD_LOCAL.get()
            newThreadLocal!!.spanId shouldMatch present()

            Response(OK)
        }

        val received = ZipkinTraces(svc(Request(Method.GET, "")))

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
            actual.traceId shouldMatch equalTo(originalTraceId)
            actual.spanId shouldMatch present()
            actual.parentSpanId shouldMatch equalTo(originalSpanId)
            Response(OK)
        }

        val originalRequest = ZipkinTraces(originalTraces, Request(Method.GET, ""))
        val actual = svc(originalRequest)
        assertThat(ZipkinTraces(actual), equalTo(originalTraces))

        assertThat(start!!.first, equalTo(originalRequest))
        assertThat(start!!.second, equalTo(originalTraces))

        assertThat(end!!.first, equalTo(originalRequest))
        assertThat(end!!.second, equalTo(ZipkinTraces(originalTraces, Response(OK))))
        assertThat(end!!.third, equalTo(originalTraces))
    }
}
