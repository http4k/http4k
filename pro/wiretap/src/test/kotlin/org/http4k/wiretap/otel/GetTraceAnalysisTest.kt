/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.sdk.trace.data.StatusData
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.Approver
import org.http4k.wiretap.HttpWiretapFunctionContract
import org.http4k.wiretap.McpWiretapFunctionContract
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.otel.breakdown.defaultTraceReportTabs
import org.junit.jupiter.api.Test

class GetTraceAnalysisTest : HttpWiretapFunctionContract, McpWiretapFunctionContract {

    override val toolName = "get_trace_analysis"

    private val traceStore = TraceStore.InMemory()

    override val function = GetTraceAnalysis(traceStore, defaultTraceReportTabs)

    private fun recordSpan(
        traceId: String,
        spanId: String = "1234567890abcdef",
        name: String = "test",
        parentSpanId: String = "0000000000000000",
        kind: SpanKind = SpanKind.SERVER,
        serviceName: String = "",
        startNanos: Long = 1000000,
        endNanos: Long = 2000000,
        status: StatusData = StatusData.ok()
    ) {
        traceStore.record(testSpanData(traceId, spanId, name, parentSpanId, kind, serviceName, startNanos, endNanos, status))
    }

    @Test
    fun `http returns diagram for multi-service trace`(approver: Approver) {
        val traceId = "00000000000000000000000000000002"
        recordSpan(traceId, spanId = "aaaaaaaaaaaaaaaa", name = "GET /", kind = SpanKind.SERVER, serviceName = "frontend", startNanos = 1000000, endNanos = 5000000)
        recordSpan(traceId, spanId = "bbbbbbbbbbbbbbbb", parentSpanId = "aaaaaaaaaaaaaaaa", name = "GET /api", kind = SpanKind.CLIENT, serviceName = "frontend", startNanos = 1500000, endNanos = 4500000)
        recordSpan(traceId, spanId = "cccccccccccccccc", parentSpanId = "bbbbbbbbbbbbbbbb", name = "GET /api", kind = SpanKind.SERVER, serviceName = "backend", startNanos = 2000000, endNanos = 4000000)
        approver.assertApproved(httpClient()(Request(GET, "/analysis/$traceId")))
    }

    @Test
    fun `http returns empty 200 for unknown trace`() {
        val response = httpClient()(Request(GET, "/analysis/00000000000000000000000000000099"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun `http returns empty 200 for single-service trace without diagram`() {
        recordSpan("00000000000000000000000000000001")
        val response = httpClient()(Request(GET, "/analysis/00000000000000000000000000000001"))
        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun `mcp returns diagram for multi-service trace`(approver: Approver) {
        val traceId = "00000000000000000000000000000002"
        recordSpan(traceId, spanId = "aaaaaaaaaaaaaaaa", name = "GET /", kind = SpanKind.SERVER, serviceName = "frontend", startNanos = 1000000, endNanos = 5000000)
        recordSpan(traceId, spanId = "bbbbbbbbbbbbbbbb", parentSpanId = "aaaaaaaaaaaaaaaa", name = "GET /api", kind = SpanKind.CLIENT, serviceName = "frontend", startNanos = 1500000, endNanos = 4500000)
        recordSpan(traceId, spanId = "cccccccccccccccc", parentSpanId = "bbbbbbbbbbbbbbbb", name = "GET /api", kind = SpanKind.SERVER, serviceName = "backend", startNanos = 2000000, endNanos = 4000000)
        approver.assertToolResponse(mapOf("trace_id" to traceId))
    }

    @Test
    fun `mcp returns empty for unknown trace`(approver: Approver) {
        approver.assertToolResponse(mapOf("trace_id" to "00000000000000000000000000000099"))
    }
}
