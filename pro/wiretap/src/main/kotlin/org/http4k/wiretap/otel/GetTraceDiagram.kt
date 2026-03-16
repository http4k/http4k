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
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.Selector
import org.http4k.lens.Path
import org.http4k.lens.datastarElements
import org.http4k.lens.value
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.OtelTraceId
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.util.Json

fun GetTraceDiagram(traceStore: TraceStore) = object : WiretapFunction {
    private fun lookup(traceId: OtelTraceId): String? {
        val spans = traceStore.get(traceId)
        if (spans.isEmpty()) return null

        val detail = spans.toTraceDetail(traceId)
        val diagram = detail.toSequenceDiagram()
        if (diagram.messages.isEmpty()) return null

        return diagram.toMermaid()
    }

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "/diagram/{traceId}" bind GET to { req ->
            val traceId = Path.value(OtelTraceId).of("traceId")(req)
            when (val mermaid = lookup(traceId)) {
                null -> Response(NOT_FOUND)
                else -> Response(OK).datastarElements(
                    elements(TraceDiagramView(mermaid)),
                    selector = Selector.of("#trace-diagram-panel")
                )
            }
        }

    override fun mcp(): ToolCapability {
        val traceId = Tool.Arg.value(OtelTraceId).required("trace_id", "The trace ID to get the sequence diagram for")

        return Tool(
            "get_trace_diagram",
            "Get the Mermaid sequence diagram for a specific OpenTelemetry trace",
            traceId
        ) bind { req ->
            when (val mermaid = lookup(traceId(req))) {
                null -> ToolResponse.Error("No diagram available for this trace")
                else -> Json.asToolResponse(mapOf("mermaid" to mermaid))
            }
        }
    }
}

data class TraceDiagramView(val mermaidDiagram: String) : ViewModel
