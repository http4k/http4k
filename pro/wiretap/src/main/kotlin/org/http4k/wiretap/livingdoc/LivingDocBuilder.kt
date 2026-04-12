/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.livingdoc

import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.Direction.Outbound
import org.http4k.wiretap.domain.Ordering
import org.http4k.wiretap.domain.OtelTraceId
import org.http4k.wiretap.domain.TraceDetail
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.domain.WiretapTransaction
import org.http4k.wiretap.domain.traceparent
import org.http4k.wiretap.otel.toTraceDetail


class LivingDocRenderer(
    private val traceStore: TraceStore,
    private val transactionStore: TransactionStore,
    private val sections: List<LivingDocSection> = defaultLivingDocSections
) {
    operator fun invoke(testName: String): String {
        val sb = StringBuilder()
        sb.appendLine("# $testName")

        val allTransactions = transactionStore.list(ordering = Ordering.Ascending, limit = Int.MAX_VALUE)
        val txByTraceId = allTransactions.groupBy { it.traceparent() }

        val traces = traceStore.traces(Ordering.Ascending)
        traces.forEach { (traceId, spans) ->
            val detail = spans.toTraceDetail(traceId)
            val traceTxs = txByTraceId[traceId] ?: emptyList()
            sb.appendTrace(detail, traceTxs)
        }

        val orphanTxs = txByTraceId.entries
            .filter { (traceId, _) -> traceId == null || traceId !in traces }
            .flatMap { it.value }

        if (orphanTxs.isNotEmpty()) {
            val inbound = orphanTxs.filter { it.direction == Inbound }
            val outbound = orphanTxs.filter { it.direction == Outbound }

            if (inbound.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("## Inbound Requests")
                inbound.forEach { sb.append(renderTransaction(it)) }
            }
            if (outbound.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("## Outbound Requests")
                outbound.forEach { sb.append(renderTransaction(it)) }
            }
        }

        return sb.toString().trimEnd() + "\n"
    }

    fun renderTrace(traceId: OtelTraceId): String? {
        val spans = traceStore.get(traceId)
        if (spans.isEmpty()) return null

        val detail = spans.toTraceDetail(traceId)
        val allTransactions = transactionStore.list(ordering = Ordering.Ascending, limit = Int.MAX_VALUE)
        val traceTxs = allTransactions.filter { it.traceparent() == traceId }

        val sb = StringBuilder()
        sb.appendTrace(detail, traceTxs)
        return sb.toString().trimEnd() + "\n"
    }

    private fun StringBuilder.appendTrace(detail: TraceDetail, transactions: List<WiretapTransaction>) {
        val mcpSpan = detail.spans.firstOrNull { span ->
            span.attributes.any { it.key == "mcp.method.name" }
        }
        val mcpTarget = mcpSpan?.attributes?.firstOrNull {
            it.key in setOf("mcp.resource.uri", "mcp.completion.ref")
        }?.value
        val traceLabel = when {
            mcpSpan != null && mcpTarget != null -> "${mcpSpan.name} $mcpTarget"
            mcpSpan != null -> mcpSpan.name
            else -> detail.spans.firstOrNull()?.name ?: "unknown"
        }

        appendLine()
        appendLine("## $traceLabel")

        sections.forEach { section ->
            val content = section.render(detail, transactions)
            if (content.isNotEmpty()) append(content)
        }
    }

    companion object {
        val defaultLivingDocSections = listOf(
            McpDetailSection,
            SequenceDiagramSection,
            SpanEventsSection,
            InboundHttpSection,
            OutboundHttpSection
        )
    }
}
