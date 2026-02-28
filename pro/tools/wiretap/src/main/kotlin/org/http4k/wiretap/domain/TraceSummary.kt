package org.http4k.wiretap.domain

data class TraceSummary(
    val traceId: String,
    val spanCount: Int,
    val rootSpanName: String,
    val serviceName: String,
    val totalDurationMs: Long,
    val timestamp: String
)

data class TraceDetail(
    val traceId: String,
    val totalDurationMs: Long,
    val spans: List<SpanDetail>
)

data class SpanDetail(
    val spanId: String,
    val parentSpanId: String,
    val name: String,
    val kind: String,
    val durationMs: Long,
    val statusCode: String,
    val statusDescription: String,
    val serviceName: String,
    val depth: Int,
    val startOffsetPercent: Double,
    val widthPercent: Double,
    val attributes: List<SpanAttribute>,
    val baggageAttributes: List<SpanAttribute>,
    val resourceAttributes: List<SpanAttribute>,
    val events: List<SpanEvent>
)

data class SpanAttribute(val key: String, val value: String)

data class SpanEvent(
    val name: String,
    val timestampMs: Long,
    val attributes: List<SpanAttribute>
)
