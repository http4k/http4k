/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.sdk.resources.Resource
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
        parentSpanId: String = "0000000000000000",
        kind: SpanKind = SpanKind.SERVER,
        serviceName: String = "",
        startNanos: Long = 1000000,
        endNanos: Long = 2000000,
        status: StatusData = StatusData.ok()
    ) {
        val builder = TestSpanData.builder()
            .setSpanContext(SpanContext.create(traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault()))
            .setParentSpanContext(
                SpanContext.create(traceId, parentSpanId, TraceFlags.getSampled(), TraceState.getDefault())
            )
            .setName(name)
            .setKind(kind)
            .setStartEpochNanos(startNanos)
            .setEndEpochNanos(endNanos)
            .setHasEnded(true)
            .setStatus(status)

        if (serviceName.isNotEmpty()) {
            builder.setResource(
                Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), serviceName))
            )
        }

        traceStore.record(builder.build())
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

    @Test
    fun `http returns trace with sequence diagram for multi-service trace`(approver: Approver) {
        val traceId = "00000000000000000000000000000002"
        recordSpan(traceId, spanId = "aaaaaaaaaaaaaaaa", name = "GET /", kind = SpanKind.SERVER, serviceName = "frontend", startNanos = 1000000, endNanos = 5000000)
        recordSpan(traceId, spanId = "bbbbbbbbbbbbbbbb", parentSpanId = "aaaaaaaaaaaaaaaa", name = "GET /api", kind = SpanKind.CLIENT, serviceName = "frontend", startNanos = 1500000, endNanos = 4500000)
        recordSpan(traceId, spanId = "cccccccccccccccc", parentSpanId = "bbbbbbbbbbbbbbbb", name = "GET /api", kind = SpanKind.SERVER, serviceName = "backend", startNanos = 2000000, endNanos = 4000000)
        approver.assertApproved(httpClient()(Request(GET, "/$traceId")))
    }
}
