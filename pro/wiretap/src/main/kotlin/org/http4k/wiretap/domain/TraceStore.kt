/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.domain

import io.opentelemetry.sdk.trace.data.SpanData
import java.util.concurrent.ConcurrentLinkedDeque

interface TraceStore {
    fun record(span: SpanData)
    fun traces(ordering: Ordering): Map<OtelTraceId, List<SpanData>>
    fun get(traceId: OtelTraceId): List<SpanData>

    companion object {
        fun InMemory(maxSpans: Int = 5000) = object : TraceStore {
            private val spans = ConcurrentLinkedDeque<SpanData>()

            override fun record(span: SpanData) {
                spans.addFirst(span)
                while (spans.size > maxSpans) {
                    spans.removeLast()
                }
            }

            override fun traces(ordering: Ordering): Map<OtelTraceId, List<SpanData>> {
                val ordered = when (ordering) {
                    Ordering.Descending -> spans.toList()
                    Ordering.Ascending -> spans.reversed()
                }
                return ordered.groupBy { OtelTraceId.of(it.traceId) }
            }

            override fun get(traceId: OtelTraceId) = spans.filter { it.traceId == traceId.value }
        }
    }
}
