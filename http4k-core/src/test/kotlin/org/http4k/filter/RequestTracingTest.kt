package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.junit.Before
import org.junit.Test

class RequestTracingTest {

    @Before
    fun before() {
        ZipkinTraces.THREAD_LOCAL.remove()
    }

    @Test
    fun `request traces are copied correctly from inbound to outbound requests`() {
        val originalTraceId = TraceId("originalTrace")
        val originalSpanId = TraceId("originalSpan")
        val originalParentSpanId = TraceId("originalParentSpanId")
        val traces = ZipkinTraces(originalTraceId, originalSpanId, originalParentSpanId)

        val client: HttpHandler = ClientFilters.RequestTracing().then {
            val actual = ZipkinTraces(it)

            actual.traceId shouldMatch equalTo(originalTraceId)
            actual.parentSpanId shouldMatch equalTo(originalSpanId)
            actual.spanId shouldMatch present()

            Response(OK)
        }

        val simpleProxyServer: HttpHandler = ServerFilters.RequestTracing().then { client(Request(Method.GET, "/somePath")) }

        val response = simpleProxyServer(ZipkinTraces(traces, Request(Method.GET, "")))

        assertThat(ZipkinTraces(response), equalTo(traces))
    }
}