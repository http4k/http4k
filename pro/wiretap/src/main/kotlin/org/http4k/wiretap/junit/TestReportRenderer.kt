/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.junit

import org.http4k.wiretap.domain.LogStore
import org.http4k.wiretap.domain.Ordering
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.domain.toDetail
import org.http4k.wiretap.domain.toSummary
import org.http4k.wiretap.otel.TraceDetailView
import org.http4k.wiretap.otel.toTraceDetail
import org.http4k.wiretap.otel.breakdown.renderTraceBreakdownView
import org.http4k.wiretap.traffic.TransactionDetailView
import org.http4k.wiretap.util.Templates
import java.time.Clock

class TestReportRenderer(
    private val traceStore: TraceStore,
    private val logStore: LogStore,
    private val transactionStore: TransactionStore,
    private val clock: Clock
) {
    operator fun invoke(testName: String, stdOut: String = "", stdErr: String = ""): String {
        val html = Templates()
        val css = TestReportRenderer::class.java.classLoader.getResourceAsStream("public/wiretap.css")
            ?.bufferedReader()?.readText() ?: ""

        val traceEntries = traceStore.traces(Ordering.Ascending).map { (traceId, spans) ->
            val detail = spans.toTraceDetail(traceId)
            val logsBySpan = logStore.forTrace(traceId).map { it.toSummary(clock) }.groupBy { it.spanId }
            TraceEntry(traceId.value, html(TraceDetailView(detail, logsBySpan)), html(html.renderTraceBreakdownView(detail)))
        }

        val trafficEntries = transactionStore.list(ordering = Ordering.Ascending, limit = Int.MAX_VALUE).map { wtx ->
            val detail = wtx.toDetail(clock)
            TrafficEntry(html(TransactionDetailView(detail)))
        }

        return html(JUnitTestReport(testName, css, traceEntries, trafficEntries, stdOut, stdErr))
    }
}
