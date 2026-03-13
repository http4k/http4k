/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.http4k.wiretap.domain.SequenceDiagram
import org.http4k.wiretap.domain.SpanAttribute
import org.http4k.wiretap.domain.SpanDetail
import org.http4k.wiretap.domain.TraceDetail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class SequenceDiagramBuilderTest {

    @Test
    fun `empty trace produces empty diagram`() {
        val trace = TraceDetail("trace1", 0L, emptyList())
        assertThat(trace.toSequenceDiagram(), equalTo(SequenceDiagram(emptyList(), emptyList())))
    }

    @Test
    fun `multi-service trace with wiretap, http status, duration, and remote authority`(approver: Approver) {
        val trace = TraceDetail(
            "trace1", 200L, listOf(
                spanDetail(
                    spanId = "root1", parentSpanId = "0000000000000000",
                    name = "GET /", kind = "SERVER", serviceName = "frontend", statusCode = "OK",
                    attributes = listOf(SpanAttribute("http.status_code", "200")),
                    durationMs = 150L
                ),
                spanDetail(
                    spanId = "client1", parentSpanId = "root1",
                    name = "GET /api", kind = "CLIENT", serviceName = "frontend", statusCode = "OK",
                    attributes = listOf(SpanAttribute("http.status_code", "200")),
                    durationMs = 80L
                ),
                spanDetail(
                    spanId = "server1", parentSpanId = "client1",
                    name = "GET /api", kind = "SERVER", serviceName = "backend", statusCode = "OK",
                    durationMs = 60L
                ),
                spanDetail(
                    spanId = "client2", parentSpanId = "server1",
                    name = "GET /ext", kind = "CLIENT", serviceName = "backend", statusCode = "OK",
                    attributes = listOf(
                        SpanAttribute("http.url", "http://api.example.com:8080/ext"),
                        SpanAttribute("http.status_code", "200")
                    ),
                    durationMs = 30L
                )
            )
        )

        approver.assertApproved(trace.toSequenceDiagram().toMermaid())
    }

    @Test
    fun `error responses and fallback labels`(approver: Approver) {
        val trace = TraceDetail(
            "trace2", 100L, listOf(
                spanDetail(
                    spanId = "root1", parentSpanId = "0000000000000000",
                    name = "GET /fail", kind = "SERVER", serviceName = "myapp", statusCode = "ERROR",
                    attributes = listOf(SpanAttribute("http.status_code", "500")),
                    durationMs = 42L
                ),
                spanDetail(
                    spanId = "client1", parentSpanId = "root1",
                    name = "GET /not-found", kind = "CLIENT", serviceName = "myapp", statusCode = "UNSET",
                    attributes = listOf(SpanAttribute("http.status_code", "404")),
                    durationMs = 15L
                ),
                spanDetail(
                    spanId = "server1", parentSpanId = "client1",
                    name = "GET /not-found", kind = "SERVER", serviceName = "downstream", statusCode = "UNSET",
                    durationMs = 10L
                ),
                spanDetail(
                    spanId = "client2", parentSpanId = "root1",
                    name = "POST /data", kind = "CLIENT", serviceName = "myapp", statusCode = "OK",
                    durationMs = 20L
                ),
                spanDetail(
                    spanId = "server2", parentSpanId = "client2",
                    name = "POST /data", kind = "SERVER", serviceName = "downstream", statusCode = "OK",
                    durationMs = 18L
                )
            )
        )

        approver.assertApproved(trace.toSequenceDiagram().toMermaid())
    }

    @Test
    fun `no root SERVER span and no wiretap`(approver: Approver) {
        val trace = TraceDetail(
            "trace3", 100L, listOf(
                spanDetail(
                    spanId = "client1", parentSpanId = "ext-parent",
                    name = "GET /api", kind = "CLIENT", serviceName = "frontend", statusCode = "OK",
                    attributes = listOf(SpanAttribute("http.status_code", "200")),
                    durationMs = 25L
                ),
                spanDetail(
                    spanId = "server1", parentSpanId = "client1",
                    name = "GET /api", kind = "SERVER", serviceName = "backend", statusCode = "OK",
                    durationMs = 20L
                )
            )
        )

        approver.assertApproved(trace.toSequenceDiagram().toMermaid())
    }

    @Test
    fun `url-based remote authority extraction`(approver: Approver) {
        val trace = TraceDetail(
            "trace4", 100L, listOf(
                spanDetail(
                    spanId = "client1", parentSpanId = "root1",
                    name = "GET /new-api", kind = "CLIENT", serviceName = "frontend", statusCode = "OK",
                    attributes = listOf(SpanAttribute("url.full", "https://backend.internal:9000/api/data")),
                    durationMs = 35L
                ),
                spanDetail(
                    spanId = "client2", parentSpanId = "root1",
                    name = "GET /legacy", kind = "CLIENT", serviceName = "frontend", statusCode = "OK",
                    attributes = listOf(SpanAttribute("http.url", "http://legacy-service/api")),
                    durationMs = 50L
                ),
                spanDetail(
                    spanId = "client3", parentSpanId = "root1",
                    name = "internal-call", kind = "CLIENT", serviceName = "frontend", statusCode = "OK",
                    durationMs = 5L
                )
            )
        )

        approver.assertApproved(trace.toSequenceDiagram().toMermaid())
    }

    @Test
    fun `special characters in service names use mermaid aliases`(approver: Approver) {
        val trace = TraceDetail(
            "trace5", 100L, listOf(
                spanDetail(
                    spanId = "client1", parentSpanId = "root1",
                    name = "GET {name:.*}", kind = "CLIENT", serviceName = "unknown_service:java",
                    statusCode = "OK", durationMs = 10L
                )
            )
        )

        approver.assertApproved(trace.toSequenceDiagram().toMermaid())
    }

    private fun spanDetail(
        spanId: String,
        parentSpanId: String,
        name: String,
        kind: String,
        serviceName: String,
        statusCode: String,
        attributes: List<SpanAttribute> = emptyList(),
        durationMs: Long = 10L
    ) = SpanDetail(
        spanId = spanId,
        parentSpanId = parentSpanId,
        name = name,
        kind = kind,
        durationMs = durationMs,
        statusCode = statusCode,
        statusDescription = "",
        serviceName = serviceName,
        depth = 0,
        startOffsetPercent = 0.0,
        widthPercent = 100.0,
        attributes = attributes,
        baggageAttributes = emptyList(),
        resourceAttributes = emptyList(),
        events = emptyList(),
        links = emptyList()
    )
}
