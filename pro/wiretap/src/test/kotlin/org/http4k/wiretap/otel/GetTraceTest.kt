package org.http4k.wiretap.otel

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.sdk.testing.trace.TestSpanData
import io.opentelemetry.sdk.trace.data.StatusData
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.testing.Approver
import org.http4k.wiretap.HttpWiretapFunctionContract
import org.http4k.wiretap.McpWiretapFunctionContract
import org.http4k.wiretap.domain.TraceStore
import org.junit.jupiter.api.Test

class GetTraceTest : HttpWiretapFunctionContract, McpWiretapFunctionContract {

    override val toolName = "get_trace"

    private val traceStore = TraceStore.InMemory()

    override val function = GetTrace(traceStore)

    private fun recordSpan(
        traceId: String,
        spanId: String = "1234567890abcdef",
        name: String = "test",
        parentSpanId: String = "0000000000000000"
    ) {
        traceStore.record(
            TestSpanData.builder()
                .setSpanContext(SpanContext.create(traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault()))
                .setParentSpanContext(
                    SpanContext.create(traceId, parentSpanId, TraceFlags.getSampled(), TraceState.getDefault())
                )
                .setName(name)
                .setKind(SpanKind.SERVER)
                .setStartEpochNanos(1000000)
                .setEndEpochNanos(2000000)
                .setHasEnded(true)
                .setStatus(StatusData.ok())
                .build()
        )
    }

    @Test
    fun `http returns trace detail`(approver: Approver) {
        recordSpan("00000000000000000000000000000001")
        approver.assertApproved(httpClient()(Request(GET, "/00000000000000000000000000000001")))
    }

    @Test
    fun `http returns 404 for unknown trace`() {
        val response = httpClient()(Request(GET, "/00000000000000000000000000000099"))
        assertThat(response.status, equalTo(NOT_FOUND))
    }

    @Test
    fun `mcp returns trace detail`(approver: Approver) {
        recordSpan("00000000000000000000000000000001")
        approver.assertToolResponse(mapOf("trace_id" to "00000000000000000000000000000001"))
    }

    @Test
    fun `mcp returns error for unknown trace`(approver: Approver) {
        approver.assertToolResponse(mapOf("trace_id" to "00000000000000000000000000000099"))
    }
}
