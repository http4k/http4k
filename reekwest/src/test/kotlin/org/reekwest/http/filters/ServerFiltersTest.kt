package org.reekwest.http.filters

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.should.shouldMatch
import org.junit.Before
import org.junit.Test
import org.reekwest.http.core.Request
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status
import org.reekwest.http.core.then

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

            Response(Status.OK)
        }

        val received = ZipkinTraces(svc(Request.get("")))

        received.traceId shouldMatch equalTo(newThreadLocal!!.traceId)
        received.spanId shouldMatch equalTo(newThreadLocal!!.parentSpanId)
        received.parentSpanId shouldMatch absent()
   }

    @Test
    fun `uses existing request tracing from request and sets on outgoing response not present`() {
        val originalTraceId = TraceId.new()
        val originalSpanId = TraceId.new()
        val originalTraces = ZipkinTraces(originalTraceId, originalSpanId, null)

        val svc = ServerFilters.RequestTracing().then {
            val actual = ZipkinTraces.THREAD_LOCAL.get()
            actual.traceId shouldMatch equalTo(originalTraceId)
            actual.spanId shouldMatch present()
            actual.parentSpanId shouldMatch equalTo(originalSpanId)
            Response(Status.OK)
        }

        val actual = svc(ZipkinTraces(originalTraces, get("")))
        assertThat(ZipkinTraces(actual), equalTo(originalTraces))
    }
}
