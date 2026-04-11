/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import org.http4k.filter.OpenTelemetrySemanticConventions
import org.http4k.wiretap.domain.OtelTraceId
import org.http4k.wiretap.domain.SpanAttribute
import org.http4k.wiretap.domain.TraceDetail
import org.junit.jupiter.api.Test

class TimingTableBuilderTest {

    @Test
    fun `empty trace produces empty table`() {
        val trace = TraceDetail(OtelTraceId.of("trace1"), 0L, emptyList())
        assertThat(trace.toTimingTable(), isEmpty)
    }

    @Test
    fun `single span is 100 percent of total`() {
        val trace = TraceDetail(
            OtelTraceId.of("trace1"), 100L, listOf(
                spanDetail(name = "GET /", kind = "SERVER", serviceName = "myapp", durationMs = 100L)
            )
        )

        assertThat(
            trace.toTimingTable(), equalTo(
                listOf(
                    TimingEntry("myapp", "GET /", "SERVER", 100L, 100.0, false)
                )
            )
        )
    }

    @Test
    fun `multiple spans sorted by duration descending`() {
        val trace = TraceDetail(
            OtelTraceId.of("trace1"), 200L, listOf(
                spanDetail(name = "GET /", kind = "SERVER", serviceName = "frontend", durationMs = 200L),
                spanDetail(name = "GET /api", kind = "CLIENT", serviceName = "frontend", durationMs = 80L),
                spanDetail(name = "GET /api", kind = "SERVER", serviceName = "backend", durationMs = 60L)
            )
        )

        val table = trace.toTimingTable()
        assertThat(table.map { it.durationMs }, equalTo(listOf(200L, 80L, 60L)))
        assertThat(table.map { it.percentOfTotal }, equalTo(listOf(100.0, 40.0, 30.0)))
    }

    @Test
    fun `error spans are flagged`() {
        val trace = TraceDetail(
            OtelTraceId.of("trace1"), 100L, listOf(
                spanDetail(name = "GET /ok", kind = "SERVER", serviceName = "myapp", durationMs = 50L, statusCode = "OK"),
                spanDetail(name = "GET /fail", kind = "SERVER", serviceName = "myapp", durationMs = 50L, statusCode = "ERROR"),
                spanDetail(
                    name = "GET /500", kind = "SERVER", serviceName = "myapp", durationMs = 30L, statusCode = "OK",
                    attributes = listOf(SpanAttribute(OpenTelemetrySemanticConventions.statusCode, "500"))
                )
            )
        )

        val table = trace.toTimingTable()
        assertThat(table.map { it.error }, equalTo(listOf(false, true, true)))
    }

    private fun spanDetail(
        name: String,
        kind: String,
        serviceName: String,
        durationMs: Long = 10L,
        statusCode: String = "OK",
        attributes: List<SpanAttribute> = emptyList()
    ) = testSpanDetail("span-${name.hashCode()}", "0000000000000000", name, kind, serviceName, statusCode, durationMs, attributes)
}
