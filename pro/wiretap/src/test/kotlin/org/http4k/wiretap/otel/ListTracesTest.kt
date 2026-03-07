package org.http4k.wiretap.otel

import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.sdk.testing.trace.TestSpanData
import io.opentelemetry.sdk.trace.data.StatusData
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.testing.Approver
import org.http4k.wiretap.HttpWiretapFunctionContract
import org.http4k.wiretap.McpWiretapFunctionContract
import org.http4k.wiretap.domain.TraceStore
import org.junit.jupiter.api.Test
import java.time.Clock

class ListTracesTest : HttpWiretapFunctionContract, McpWiretapFunctionContract {

    override val toolName = "list_traces"

    private val traceStore = TraceStore.InMemory()

    override val function = ListTraces(traceStore, Clock.systemUTC())

    private fun recordSpan(traceId: String, spanId: String = "1234567890abcdef", name: String = "test") {
        traceStore.record(
            TestSpanData.builder()
                .setSpanContext(SpanContext.create(traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault()))
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
    fun `http returns empty list`(approver: Approver) {
        approver.assertApproved(httpClient()(Request(GET, "/list")))
    }

    @Test
    fun `http lists traces`(approver: Approver) {
        recordSpan("00000000000000000000000000000001")
        approver.assertApproved(httpClient()(Request(GET, "/list")))
    }

    @Test
    fun `mcp returns empty list`(approver: Approver) {
        approver.assertToolResponse(emptyMap())
    }

    @Test
    fun `mcp lists traces`(approver: Approver) {
        recordSpan("00000000000000000000000000000001")
        approver.assertToolResponse(emptyMap())
    }
}
