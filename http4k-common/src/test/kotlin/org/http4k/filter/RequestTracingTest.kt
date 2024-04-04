package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.SamplingDecision.Companion.DO_NOT_SAMPLE
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors

class RequestTracingTest {

    @BeforeEach
    fun before() {
        ZipkinTracesStorage.INTERNAL_THREAD_LOCAL.remove()
    }

    @Test
    fun `request traces are copied correctly from inbound to outbound requests`() {
        val originalTraceId = TraceId("originalTrace")
        val originalSpanId = TraceId("originalSpan")
        val originalParentSpanId = TraceId("originalParentSpanId")
        val traces = ZipkinTraces(originalTraceId, originalSpanId, originalParentSpanId, DO_NOT_SAMPLE)

        val client: HttpHandler = ClientFilters.RequestTracing().then {
            val actual = ZipkinTraces(it)

            assertThat(actual.traceId, equalTo(originalTraceId))
            assertThat(actual.parentSpanId, equalTo(originalSpanId))
            assertThat(actual.spanId, present())
            assertThat(actual.samplingDecision, equalTo(DO_NOT_SAMPLE))

            Response(OK)
        }

        val simpleProxyServer: HttpHandler = ServerFilters.RequestTracing().then { client(Request(GET, "/somePath")) }

        val response = simpleProxyServer(ZipkinTraces(traces, Request(GET, "")))

        assertThat(ZipkinTraces(response), equalTo(traces))
    }

    @Test
    fun `client should create new span_id even if parent null`() {
        val cliWithEvents = ClientFilters.RequestTracing()
            .then {
                val actual = ZipkinTraces(it)
                assertThat(actual.parentSpanId, equalTo(TraceId("span_id")))
                Response(OK)
            }
        ZipkinTracesStorage.THREAD_LOCAL
            .setForCurrentThread(ZipkinTraces(TraceId("trace_id"), TraceId("span_id"), null))
        cliWithEvents(Request(GET, "/parentNull"))
        ZipkinTracesStorage.THREAD_LOCAL
            .setForCurrentThread(ZipkinTraces(TraceId("trace_id"), TraceId("span_id"), TraceId("parent_id")))
        cliWithEvents(Request(GET, "/parentNotNull"))
    }

    @Test
    fun `request traces may be copied to child threads`() {
        val originalTraceId = TraceId("originalTrace")
        val originalSpanId = TraceId("originalSpan")
        val originalParentSpanId = TraceId("originalParentSpanId")
        val traces = ZipkinTraces(originalTraceId, originalSpanId, originalParentSpanId, DO_NOT_SAMPLE)

        val client: HttpHandler = ClientFilters.RequestTracing().then {
            val actual = ZipkinTraces(it)

            assertThat(actual.traceId, equalTo(originalTraceId))
            assertThat(actual.parentSpanId, equalTo(originalSpanId))
            assertThat(actual.spanId, present())
            assertThat(actual.samplingDecision, equalTo(DO_NOT_SAMPLE))

            Response(OK)
        }

        val executor = Executors.newSingleThreadExecutor()
        val storage = ZipkinTracesStorage.THREAD_LOCAL

        val simpleProxyServer: HttpHandler = ServerFilters.RequestTracing().then {
            val traceForOuterThread = storage.forCurrentThread()
            val clientTask = {
                storage.setForCurrentThread(traceForOuterThread)
                client(Request(GET, "/somePath"))
            }
            executor.submit(clientTask).get()
        }

        val response = simpleProxyServer(ZipkinTraces(traces, Request(GET, "")))

        assertThat(ZipkinTraces(response), equalTo(traces))
    }
}
