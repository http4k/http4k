/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.filter.OpenTelemetrySemanticConventions.statusCode
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.http4k.wiretap.domain.OtelTraceId
import org.http4k.wiretap.domain.SpanAttribute
import org.http4k.wiretap.domain.TraceDetail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class ErrorTraceBuilderTest {

    @Test
    fun `no error spans produces empty diagram`() {
        val trace = TraceDetail(
            OtelTraceId.of("trace1"), 100L, listOf(
                spanDetail(spanId = "root1", parentSpanId = "0000000000000000", name = "GET /", kind = "SERVER", serviceName = "myapp", statusCode = "OK"),
                spanDetail(spanId = "client1", parentSpanId = "root1", name = "GET /api", kind = "CLIENT", serviceName = "myapp", statusCode = "OK",
                    attributes = listOf(SpanAttribute(statusCode, "200"))),
                spanDetail(spanId = "server1", parentSpanId = "client1", name = "GET /api", kind = "SERVER", serviceName = "backend", statusCode = "OK")
            )
        )
        assertThat(trace.toErrorTrace(), equalTo(""))
    }

    @Test
    fun `error span deep in tree shows path from root to error`(approver: Approver) {
        val trace = TraceDetail(
            OtelTraceId.of("trace1"), 200L, listOf(
                spanDetail(spanId = "root1", parentSpanId = "0000000000000000", name = "GET /", kind = "SERVER", serviceName = "frontend", statusCode = "OK", durationMs = 200L),
                spanDetail(spanId = "client1", parentSpanId = "root1", name = "GET /api", kind = "CLIENT", serviceName = "frontend", statusCode = "OK", durationMs = 100L,
                    attributes = listOf(SpanAttribute(statusCode, "200"))),
                spanDetail(spanId = "server1", parentSpanId = "client1", name = "GET /api", kind = "SERVER", serviceName = "backend", statusCode = "OK", durationMs = 80L),
                spanDetail(spanId = "client2", parentSpanId = "server1", name = "GET /db", kind = "CLIENT", serviceName = "backend", statusCode = "ERROR", durationMs = 30L,
                    attributes = listOf(SpanAttribute(statusCode, "500"))),
                spanDetail(spanId = "server2", parentSpanId = "client2", name = "GET /db", kind = "SERVER", serviceName = "database", statusCode = "ERROR", durationMs = 25L),
                // unrelated successful branch - should be excluded
                spanDetail(spanId = "client3", parentSpanId = "root1", name = "GET /cache", kind = "CLIENT", serviceName = "frontend", statusCode = "OK", durationMs = 10L,
                    attributes = listOf(SpanAttribute(statusCode, "200"))),
                spanDetail(spanId = "server3", parentSpanId = "client3", name = "GET /cache", kind = "SERVER", serviceName = "cache", statusCode = "OK", durationMs = 5L)
            )
        )

        approver.assertApproved(trace.toErrorTrace())
    }

    @Test
    fun `multiple error paths are both included`(approver: Approver) {
        val trace = TraceDetail(
            OtelTraceId.of("trace1"), 200L, listOf(
                spanDetail(spanId = "root1", parentSpanId = "0000000000000000", name = "GET /", kind = "SERVER", serviceName = "frontend", statusCode = "OK", durationMs = 200L),
                spanDetail(spanId = "client1", parentSpanId = "root1", name = "GET /api", kind = "CLIENT", serviceName = "frontend", statusCode = "ERROR", durationMs = 100L,
                    attributes = listOf(SpanAttribute(statusCode, "500"))),
                spanDetail(spanId = "server1", parentSpanId = "client1", name = "GET /api", kind = "SERVER", serviceName = "backend", statusCode = "ERROR", durationMs = 80L),
                spanDetail(spanId = "client2", parentSpanId = "root1", name = "GET /other", kind = "CLIENT", serviceName = "frontend", statusCode = "ERROR", durationMs = 50L,
                    attributes = listOf(SpanAttribute(statusCode, "503"))),
                spanDetail(spanId = "server2", parentSpanId = "client2", name = "GET /other", kind = "SERVER", serviceName = "other-service", statusCode = "ERROR", durationMs = 40L)
            )
        )

        approver.assertApproved(trace.toErrorTrace())
    }

    private fun spanDetail(
        spanId: String,
        parentSpanId: String,
        name: String,
        kind: String,
        serviceName: String,
        statusCode: String = "OK",
        durationMs: Long = 10L,
        attributes: List<SpanAttribute> = emptyList()
    ) = testSpanDetail(spanId, parentSpanId, name, kind, serviceName, statusCode, durationMs, attributes)
}
