/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import org.http4k.wiretap.domain.OtelSpanId
import org.http4k.wiretap.domain.SpanDetail
import org.http4k.wiretap.domain.TraceDetail

private val ROOT_PARENT_SPAN_ID = OtelSpanId.of("0000000000000000")

fun TraceDetail.toCriticalPath(): String {
    if (spans.size < 2) return ""

    val spanById = spans.associateBy { it.spanId }
    val childrenOf = spans.groupBy { it.parentSpanId }

    val roots = spans.filter {
        it.parentSpanId == ROOT_PARENT_SPAN_ID || it.parentSpanId !in spanById
    }
    if (roots.isEmpty()) return ""

    fun longestPath(span: SpanDetail): List<SpanDetail> {
        val children = childrenOf[span.spanId] ?: return listOf(span)
        val longestChild = children.maxByOrNull { child -> longestPathDuration(child, childrenOf) }
            ?: return listOf(span)
        return listOf(span) + longestPath(longestChild)
    }

    val criticalPath = roots.maxByOrNull { longestPathDuration(it, childrenOf) }
        ?.let { longestPath(it) }
        ?: return ""

    val criticalSpanIds = criticalPath.map { it.spanId }.toSet()
    val filtered = copy(spans = spans.filter { it.spanId in criticalSpanIds })
    val diagram = filtered.toSequenceDiagram()
    return if (diagram.messages.isNotEmpty()) diagram.toMermaid() else ""
}

private fun longestPathDuration(span: SpanDetail, childrenOf: Map<OtelSpanId, List<SpanDetail>>): Long {
    val children = childrenOf[span.spanId] ?: return span.durationMs
    val maxChild = children.maxOfOrNull { longestPathDuration(it, childrenOf) } ?: 0L
    return span.durationMs + maxChild
}
