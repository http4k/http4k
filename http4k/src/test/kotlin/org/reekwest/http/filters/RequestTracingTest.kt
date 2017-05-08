package org.http4k.http.filters

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.should.shouldMatch
import org.junit.Before
import org.junit.Test
import org.http4k.http.core.HttpHandler
import org.http4k.http.core.Request.Companion.get
import org.http4k.http.core.Response
import org.http4k.http.core.Status.Companion.OK
import org.http4k.http.core.then

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

        val simpleProxyServer: HttpHandler = ServerFilters.RequestTracing().then { client(get("/somePath")) }

        val response = simpleProxyServer(ZipkinTraces(traces, get("")))

        assertThat(ZipkinTraces(response), equalTo(traces))
    }
}