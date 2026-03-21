/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import io.opentelemetry.api.common.AttributeKey
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.MorphMode.inner
import org.http4k.datastar.Selector
import org.http4k.lens.datastarElements
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.Ordering.Descending
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TraceSummary
import org.http4k.wiretap.util.Json
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter.ofPattern

fun ListTraces(traceStore: TraceStore, clock: Clock) = object : WiretapFunction {
    private fun list(): List<TraceSummary> =
        traceStore.traces(Descending).map { (traceId, spans) ->
            val rootSpan = spans.find { it.parentSpanId == "0000000000000000" } ?: spans.firstOrNull()
            val earliest = spans.minOfOrNull { it.startEpochNanos } ?: 0L
            val latest = spans.maxOfOrNull { it.endEpochNanos } ?: 0L
            TraceSummary(
                traceId,
                spans.size,
                rootSpan?.name ?: "unknown",
                rootSpan?.resource?.attributes?.get(AttributeKey.stringKey("service.name")) ?: "",
                (latest - earliest) / 1_000_000,
                ofPattern("HH:mm:ss.SSS").withZone(clock.zone).format(Instant.ofEpochSecond(0, earliest))
            )
        }

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "/list" bind GET to {
            Response(OK).datastarElements(
                list().map { TraceRowView(it) }.flatMap { elements(it) },
                inner,
                Selector.of("#trace-list")
            )
        }

    override fun mcp(): ToolCapability = Tool(
        "list_traces",
        "List OpenTelemetry traces with trace ID, span count, root span name, and duration"
    ) bind {
        Json.asToolResponse(list())
    }
}

data class TraceRowView(val trace: TraceSummary) : ViewModel {
    val shortTraceId = trace.traceId.value.takeLast(8)
}
