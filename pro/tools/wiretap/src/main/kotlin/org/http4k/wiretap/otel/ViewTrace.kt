package org.http4k.wiretap.otel

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.trace.data.SpanData
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.Selector
import org.http4k.lens.Path
import org.http4k.lens.datastarElements
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.SpanAttribute
import org.http4k.wiretap.domain.SpanDetail
import org.http4k.wiretap.domain.SpanEvent
import org.http4k.wiretap.domain.TraceDetail
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.util.Json
import kotlin.math.max

fun GetTrace(traceStore: TraceStore) = object : WiretapFunction {
    private fun lookup(traceId: String): TraceDetail? {
        val spans = traceStore.get(traceId)
        if (spans.isEmpty()) return null

        val traceStartNanos = spans.minOf { it.startEpochNanos }
        val traceEndNanos = spans.maxOf { it.endEpochNanos }
        val traceDurationNanos = max(1L, traceEndNanos - traceStartNanos)

        val byParent = spans.groupBy { it.parentSpanId }
        val flatSpans = buildSpanTree(spans, byParent, traceStartNanos, traceDurationNanos)

        return TraceDetail(traceId, flatSpans.maxOfOrNull { it.durationMs } ?: 0L, flatSpans)
    }

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "/{traceId}" bind GET to { req ->
            when (val detail = lookup(Path.of("traceId")(req))) {
                null -> Response(NOT_FOUND)
                else -> Response(OK).datastarElements(
                    elements(TraceDetailView(detail)),
                    selector = Selector.of("#trace-detail-panel")
                )
            }
        }

    override fun mcp(): ToolCapability {
        val traceId = Tool.Arg.string().required("trace_id", "The trace ID to look up")

        return Tool(
            "get_trace",
            "Get the span tree for a specific OpenTelemetry trace",
            traceId
        ) bind { req ->
            when (val detail = lookup(traceId(req))) {
                null -> Error("Trace not found")
                else -> Json.asToolResponse(detail)
            }
        }
    }
}

data class SpanNodeView(val span: SpanDetail) : ViewModel

data class TraceDetailView(val detail: TraceDetail) : ViewModel {
    val shortTraceId = detail.traceId.takeLast(8)
    val spans = detail.spans.map { SpanNodeView(it) }
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
    val allAttributes = span.attributes.asMap().map { (k, v) -> SpanAttribute(k.key, v.toString()) }
    val detail = SpanDetail(
        spanId = span.spanId,
        parentSpanId = span.parentSpanId,
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
        resourceAttributes = span.resource.attributes.asMap().map { (k, v) -> SpanAttribute(k.key, v.toString()) },
        events = span.events.map { event ->
            SpanEvent(
                name = event.name,
                timestampMs = (event.epochNanos - traceStartNanos) / 1_000_000,
                attributes = event.attributes.asMap().map { (k, v) -> SpanAttribute(k.key, v.toString()) }
            )
        }
    )
    val children = buildNodes(span.spanId, depth + 1, byParent, traceStartNanos, traceDurationNanos)
    return listOf(detail) + children
}
