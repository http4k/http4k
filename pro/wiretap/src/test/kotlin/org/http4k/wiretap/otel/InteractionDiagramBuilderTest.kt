/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.filter.OpenTelemetrySemanticConventions
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.http4k.wiretap.domain.OtelTraceId
import org.http4k.wiretap.domain.SpanAttribute
import org.http4k.wiretap.domain.TraceDetail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class InteractionDiagramBuilderTest {

    @Test
    fun `empty trace produces empty diagram`() {
        val trace = TraceDetail(OtelTraceId.of("trace1"), 0L, emptyList())
        assertThat(trace.toInteractionDiagram(), equalTo(""))
    }

    @Test
    fun `single client-server interaction`(approver: Approver) {
        val trace = TraceDetail(
            OtelTraceId.of("trace1"), 100L, listOf(
                spanDetail(spanId = "client1", parentSpanId = "root1", name = "GET /api", kind = "CLIENT", serviceName = "frontend"),
                spanDetail(spanId = "server1", parentSpanId = "client1", name = "GET /api", kind = "SERVER", serviceName = "backend")
            )
        )

        approver.assertApproved(trace.toInteractionDiagram())
    }

    @Test
    fun `duplicate relationships are deduplicated`(approver: Approver) {
        val trace = TraceDetail(
            OtelTraceId.of("trace1"), 200L, listOf(
                spanDetail(spanId = "client1", parentSpanId = "root1", name = "GET /api", kind = "CLIENT", serviceName = "frontend"),
                spanDetail(spanId = "server1", parentSpanId = "client1", name = "GET /api", kind = "SERVER", serviceName = "backend"),
                spanDetail(spanId = "client2", parentSpanId = "root1", name = "POST /api", kind = "CLIENT", serviceName = "frontend"),
                spanDetail(spanId = "server2", parentSpanId = "client2", name = "POST /api", kind = "SERVER", serviceName = "backend")
            )
        )

        approver.assertApproved(trace.toInteractionDiagram())
    }

    @Test
    fun `multiple services with different relationships`(approver: Approver) {
        val trace = TraceDetail(
            OtelTraceId.of("trace1"), 300L, listOf(
                spanDetail(spanId = "root1", parentSpanId = "0000000000000000", name = "GET /", kind = "SERVER", serviceName = "frontend"),
                spanDetail(spanId = "client1", parentSpanId = "root1", name = "GET /api", kind = "CLIENT", serviceName = "frontend"),
                spanDetail(spanId = "server1", parentSpanId = "client1", name = "GET /api", kind = "SERVER", serviceName = "backend"),
                spanDetail(spanId = "client2", parentSpanId = "server1", name = "SELECT", kind = "CLIENT", serviceName = "backend"),
                spanDetail(
                    spanId = "client3", parentSpanId = "server1", name = "GET /ext", kind = "CLIENT", serviceName = "backend",
                    attributes = listOf(SpanAttribute(OpenTelemetrySemanticConventions.clientUrl, "http://api.example.com/ext"))
                )
            )
        )

        approver.assertApproved(trace.toInteractionDiagram())
    }

    @Test
    fun `falls back to remote authority when no child server span`(approver: Approver) {
        val trace = TraceDetail(
            OtelTraceId.of("trace1"), 100L, listOf(
                spanDetail(
                    spanId = "client1", parentSpanId = "root1", name = "GET /ext", kind = "CLIENT", serviceName = "myapp",
                    attributes = listOf(SpanAttribute(OpenTelemetrySemanticConventions.clientUrl, "https://external.api.com:8080/data"))
                )
            )
        )

        approver.assertApproved(trace.toInteractionDiagram())
    }

    private fun spanDetail(
        spanId: String,
        parentSpanId: String,
        name: String,
        kind: String,
        serviceName: String,
        attributes: List<SpanAttribute> = emptyList()
    ) = testSpanDetail(spanId, parentSpanId, name, kind, serviceName, attributes = attributes)
}
