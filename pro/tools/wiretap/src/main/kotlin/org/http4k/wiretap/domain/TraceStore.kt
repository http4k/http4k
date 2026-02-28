package org.http4k.wiretap.domain

import io.opentelemetry.sdk.trace.data.SpanData
import java.util.concurrent.ConcurrentLinkedDeque

interface TraceStore {
    fun record(span: SpanData)
    fun list(): List<SpanData>
    fun traces(): Map<String, List<SpanData>>
    fun get(traceId: String): List<SpanData>
    fun clear()

    companion object {
        fun InMemory(maxSpans: Int = 5000) = object : TraceStore {
            private val spans = ConcurrentLinkedDeque<SpanData>()

            override fun record(span: SpanData) {
                spans.addFirst(span)
                while (spans.size > maxSpans) {
                    spans.removeLast()
                }
            }

            override fun list(): List<SpanData> = spans.toList()

            override fun traces(): Map<String, List<SpanData>> =
                spans.groupBy { it.traceId }

            override fun get(traceId: String): List<SpanData> =
                spans.filter { it.traceId == traceId }

            override fun clear() {
                spans.clear()
            }
        }
    }
}
