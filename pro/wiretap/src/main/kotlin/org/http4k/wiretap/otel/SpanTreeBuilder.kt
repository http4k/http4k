/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.trace.data.SpanData
import org.http4k.wiretap.domain.OtelSpanId
import org.http4k.wiretap.domain.OtelTraceId
import org.http4k.wiretap.domain.SpanAttribute
import org.http4k.wiretap.domain.SpanDetail
import org.http4k.wiretap.domain.SpanEvent
import org.http4k.wiretap.domain.SpanLink
import org.http4k.wiretap.domain.TraceDetail
import kotlin.math.max

private fun Any.toAttributeValue(): String = when (this) {
    is Array<*> -> contentToString()
    else -> toString()
}

private fun Attributes.toSpanAttributes() = asMap().map { (k, v) -> SpanAttribute(k.key, v.toAttributeValue()) }

fun List<SpanData>.toTraceDetail(traceId: OtelTraceId): TraceDetail {
    val traceStartNanos = minOf { it.startEpochNanos }
    val traceEndNanos = maxOf { it.endEpochNanos }
    val traceDurationNanos = max(1L, traceEndNanos - traceStartNanos)

    val byParent = groupBy { it.parentSpanId }
    val flatSpans = buildSpanTree(this, byParent, traceStartNanos, traceDurationNanos)

    return TraceDetail(traceId, flatSpans.maxOfOrNull { it.durationMs } ?: 0L, flatSpans)
}

private fun buildSpanTree(
    spans: List<SpanData>,
    byParent: Map<String, List<SpanData>>,
    traceStartNanos: Long,
    traceDurationNanos: Long
): List<SpanDetail> {
    val rootParentId = "0000000000000000"
    val roots = buildNodes(rootParentId, 0, byParent, traceStartNanos, traceDurationNanos)
    if (roots.isNotEmpty()) return roots

    val allSpanIds = spans.map { it.spanId }.toSet()
    val orphanRoots = spans.filter { it.parentSpanId !in allSpanIds }

    return orphanRoots.sortedBy { it.startEpochNanos }.flatMap {
        flattenSpan(it, 0, byParent, traceStartNanos, traceDurationNanos)
    }
}

private fun buildNodes(
    parentId: String,
    depth: Int,
    byParent: Map<String, List<SpanData>>,
    traceStartNanos: Long,
    traceDurationNanos: Long
): List<SpanDetail> =
    (byParent[parentId] ?: emptyList())
        .sortedBy { it.startEpochNanos }
        .flatMap { flattenSpan(it, depth, byParent, traceStartNanos, traceDurationNanos) }

private fun flattenSpan(
    span: SpanData,
    depth: Int,
    byParent: Map<String, List<SpanData>>,
    traceStartNanos: Long,
    traceDurationNanos: Long
): List<SpanDetail> {
    val allAttributes = span.attributes.toSpanAttributes()
    val detail = SpanDetail(
        spanId = OtelSpanId.of(span.spanId),
        parentSpanId = OtelSpanId.of(span.parentSpanId),
        name = span.name,
        kind = span.kind.name,
        durationMs = (span.endEpochNanos - span.startEpochNanos) / 1_000_000,
        statusCode = span.status.statusCode.name,
        statusDescription = span.status.description ?: "",
        serviceName = span.resource.attributes.get(AttributeKey.stringKey("service.name")) ?: "",
        depth = depth,
        startOffsetPercent = ((span.startEpochNanos - traceStartNanos).toDouble() / traceDurationNanos) * 100.0,
        widthPercent = max(0.5, ((span.endEpochNanos - span.startEpochNanos).toDouble() / traceDurationNanos) * 100.0),
        attributes = allAttributes.filterNot { it.key.startsWith("baggage.") },
        baggageAttributes = allAttributes
            .filter { it.key.startsWith("baggage.") }
            .map { it.copy(key = it.key.removePrefix("baggage.")) },
        resourceAttributes = span.resource.attributes.toSpanAttributes(),
        events = span.events.map { event ->
            SpanEvent(
                name = event.name,
                timestampMs = (event.epochNanos - traceStartNanos) / 1_000_000,
                attributes = event.attributes.toSpanAttributes()
            )
        },
        links = span.links.map { link ->
            SpanLink(
                traceId = OtelTraceId.of(link.spanContext.traceId),
                spanId = OtelSpanId.of(link.spanContext.spanId),
                attributes = link.attributes.toSpanAttributes()
            )
        }
    )
    val children = buildNodes(span.spanId, depth + 1, byParent, traceStartNanos, traceDurationNanos)
    return listOf(detail) + children
}
