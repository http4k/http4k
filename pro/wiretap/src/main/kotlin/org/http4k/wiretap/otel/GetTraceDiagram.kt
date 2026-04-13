/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.value
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.Selector
import org.http4k.lens.Path
import org.http4k.lens.datastarElements
import org.http4k.lens.value
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.OtelTraceId
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.otel.breakdown.TabContentRenderer
import org.http4k.wiretap.otel.breakdown.TraceBreakdownView
import org.http4k.wiretap.otel.breakdown.renderTraceBreakdownView
import org.http4k.wiretap.util.Json

fun GetTraceDiagrams(traceStore: TraceStore, traceReportTabs: List<TabContentRenderer>) = object : WiretapFunction {
    private fun lookup(traceId: OtelTraceId, html: TemplateRenderer): TraceBreakdownView? {
        val spans = traceStore.get(traceId)
        if (spans.isEmpty()) return null
        return html.renderTraceBreakdownView(spans.toTraceDetail(traceId), traceReportTabs)
    }

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "/diagrams/{traceId}" bind GET to { req ->
            val traceId = Path.value(OtelTraceId).of("traceId")(req)
            when (val view = lookup(traceId, html)) {
                null -> Response(OK)
                else -> Response(OK).datastarElements(
                    elements(view),
                    selector = Selector.of("#trace-diagrams-panel")
                )
            }
        }

    override fun mcp(): ToolCapability {
        val traceId = Tool.Arg.value(OtelTraceId).required("trace_id", "The trace ID to get diagrams for")

        return Tool(
            "get_trace_diagrams",
            "Get all diagrams (sequence, interaction, timing, errors, critical path) for a specific OpenTelemetry trace",
            traceId
        ) bind { req ->
            val detail = traceStore.get(traceId(req)).let { spans ->
                if (spans.isEmpty()) null else spans.toTraceDetail(traceId(req))
            }
            when (detail) {
                null -> ToolResponse.Error("No trace data available")
                else -> Json.asToolResponse(
                    mapOf(
                        "sequence" to detail.toSequenceDiagram().toMermaid(),
                        "interaction" to detail.toInteractionDiagram(),
                        "timing" to detail.toTimingTable(),
                        "errorTrace" to detail.toErrorTrace(),
                        "criticalPath" to detail.toCriticalPath()
                    ).filterValues { it is String && it.isNotEmpty() || it is List<*> && it.isNotEmpty() }
                )
            }
        }
    }
}
