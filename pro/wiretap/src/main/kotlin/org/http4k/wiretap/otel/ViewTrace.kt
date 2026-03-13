/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

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
import org.http4k.wiretap.domain.LogStore
import org.http4k.wiretap.domain.LogSummary
import org.http4k.wiretap.domain.SpanDetail
import org.http4k.wiretap.domain.TraceDetail
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.toSummary
import org.http4k.wiretap.util.Json
import java.time.Clock

fun GetTrace(traceStore: TraceStore, logStore: LogStore, clock: Clock) = object : WiretapFunction {
    private fun lookup(traceId: String): TraceDetail? {
        val spans = traceStore.get(traceId)
        if (spans.isEmpty()) return null
        return spans.toTraceDetail(traceId)
    }

    private fun logsForTrace(traceId: String): Map<String, List<LogSummary>> =
        logStore.forTrace(traceId).map { it.toSummary(clock) }.groupBy { it.spanId ?: "" }

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "/{traceId}" bind GET to { req ->
            val traceId = Path.of("traceId")(req)
            when (val detail = lookup(traceId)) {
                null -> Response(NOT_FOUND)
                else -> Response(OK).datastarElements(
                    elements(TraceDetailView(detail, logsForTrace(traceId))),
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

data class LogRowView(val log: LogSummary, val columnValues: List<String>)

data class SpanNodeView(val span: SpanDetail, val logs: List<LogSummary> = emptyList()) : ViewModel {
    val logAttrColumns = logs.flatMap { it.attributes.map { a -> a.key } }.distinct()
    val logRows = logs.map { log ->
        val attrMap = log.attributes.associate { it.key to it.value }
        LogRowView(log, logAttrColumns.map { attrMap[it] ?: "" })
    }
}

data class TraceDetailView(val detail: TraceDetail, val logsBySpan: Map<String, List<LogSummary>> = emptyMap()) : ViewModel {
    val shortTraceId = detail.traceId.takeLast(8)
    val spans = detail.spans.map { SpanNodeView(it, logsBySpan[it.spanId] ?: emptyList()) }
    val orphanLogs = logsBySpan[""] ?: emptyList()
    val orphanLogAttrColumns = orphanLogs.flatMap { it.attributes.map { a -> a.key } }.distinct()
    val orphanLogRows = orphanLogs.map { log ->
        val attrMap = log.attributes.associate { it.key to it.value }
        LogRowView(log, orphanLogAttrColumns.map { attrMap[it] ?: "" })
    }
}
