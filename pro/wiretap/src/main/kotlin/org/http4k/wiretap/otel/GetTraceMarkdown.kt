/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.value
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.Path
import org.http4k.lens.value
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.OtelTraceId
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.livingdoc.LivingDocRenderer
import org.http4k.wiretap.livingdoc.LivingDocSection
import org.http4k.wiretap.util.Json

fun GetTraceMarkdown(traceStore: TraceStore,
                     transactionStore: TransactionStore,
                     livingDocSections: List<LivingDocSection>) = object : WiretapFunction {
    private val renderer = LivingDocRenderer(traceStore, transactionStore, livingDocSections)

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "/markdown/{traceId}" bind GET to { req ->
            val traceId = Path.value(OtelTraceId).of("traceId")(req)
            when (val md = renderer.renderTrace(traceId)) {
                null -> Response(NOT_FOUND)
                else -> Response(OK)
                    .with(CONTENT_TYPE of TEXT_PLAIN)
                    .body(md)
            }
        }

    override fun mcp(): ToolCapability {
        val traceId = Tool.Arg.value(OtelTraceId).required("trace_id", "The trace ID to export as markdown")
        return Tool(
            "export_trace_markdown",
            "Export a trace as a markdown living document with sequence diagram and HTTP transactions",
            traceId
        ) bind { req ->
            when (val md = renderer.renderTrace(traceId(req))) {
                null -> ToolResponse.Error("No trace data available")
                else -> Json.asToolResponse(mapOf("markdown" to md))
            }
        }
    }
}
