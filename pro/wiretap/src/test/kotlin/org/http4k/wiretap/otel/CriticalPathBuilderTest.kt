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
class CriticalPathBuilderTest {

    @Test
    fun `single span produces empty diagram`() {
        val trace = TraceDetail(
            OtelTraceId.of("trace1"), 100L, listOf(
                spanDetail(spanId = "root1", parentSpanId = "0000000000000000", name = "GET /", kind = "SERVER", serviceName = "myapp", durationMs = 100L)
            )
        )
        assertThat(trace.toCriticalPath(), equalTo(""))
    }

    @Test
    fun `linear chain is the critical path`(approver: Approver) {
        val trace = TraceDetail(
            OtelTraceId.of("trace1"), 200L, listOf(
                spanDetail(spanId = "root1", parentSpanId = "0000000000000000", name = "GET /", kind = "SERVER", serviceName = "frontend", durationMs = 200L),
                spanDetail(spanId = "client1", parentSpanId = "root1", name = "GET /api", kind = "CLIENT", serviceName = "frontend", durationMs = 100L,
                    attributes = listOf(SpanAttribute(statusCode, "200"))),
                spanDetail(spanId = "server1", parentSpanId = "client1", name = "GET /api", kind = "SERVER", serviceName = "backend", durationMs = 80L)
            )
        )

        approver.assertApproved(trace.toCriticalPath())
    }

    @Test
    fun `picks the slower branch`(approver: Approver) {
        val trace = TraceDetail(
            OtelTraceId.of("trace1"), 300L, listOf(
                spanDetail(spanId = "root1", parentSpanId = "0000000000000000", name = "GET /", kind = "SERVER", serviceName = "frontend", durationMs = 300L),
                // slow branch: frontend -> backend -> database (100 + 80 + 60 = 240)
                spanDetail(spanId = "client1", parentSpanId = "root1", name = "GET /api", kind = "CLIENT", serviceName = "frontend", durationMs = 100L,
                    attributes = listOf(SpanAttribute(statusCode, "200"))),
                spanDetail(spanId = "server1", parentSpanId = "client1", name = "GET /api", kind = "SERVER", serviceName = "backend", durationMs = 80L),
                spanDetail(spanId = "client2", parentSpanId = "server1", name = "SELECT", kind = "CLIENT", serviceName = "backend", durationMs = 60L,
                    attributes = listOf(SpanAttribute(statusCode, "200"))),
                spanDetail(spanId = "server2", parentSpanId = "client2", name = "SELECT", kind = "SERVER", serviceName = "database", durationMs = 50L),
                // fast branch: frontend -> cache (10 + 5 = 15)
                spanDetail(spanId = "client3", parentSpanId = "root1", name = "GET /cache", kind = "CLIENT", serviceName = "frontend", durationMs = 10L,
                    attributes = listOf(SpanAttribute(statusCode, "200"))),
                spanDetail(spanId = "server3", parentSpanId = "client3", name = "GET /cache", kind = "SERVER", serviceName = "cache", durationMs = 5L)
            )
        )

        approver.assertApproved(trace.toCriticalPath())
    }

    private fun spanDetail(
        spanId: String,
        parentSpanId: String,
        name: String,
        kind: String,
        serviceName: String,
        durationMs: Long = 10L,
        attributes: List<SpanAttribute> = emptyList()
    ) = testSpanDetail(spanId, parentSpanId, name, kind, serviceName, durationMs = durationMs, attributes = attributes)
}
