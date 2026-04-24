/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind.SERVER
import io.opentelemetry.api.trace.StatusCode.ERROR
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcResponse
import org.http4k.ai.mcp.server.protocol.McpFilter
import org.http4k.ai.mcp.server.protocol.McpResponse
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.parse
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.lens.Header
import org.http4k.lens.MCP_PROTOCOL_VERSION
import org.http4k.metrics.Http4kOpenTelemetry.INSTRUMENTATION_NAME

/**
 * OpenTelemetry tracing for MCP servers. Follows the latest conventions from the OTel spec.
 */
fun McpFilters.OpenTelemetryTracing(
    openTelemetry: OpenTelemetry = GlobalOpenTelemetry.get(),
    spanModifiers: List<McpOpenTelemetrySpanModifier> = defaultMcpOtelSpanModifiers
): McpFilter {
    val tracer = openTelemetry.tracerProvider.get(INSTRUMENTATION_NAME)
    val textMapPropagator = openTelemetry.propagators.textMapPropagator

    return McpFilter { next ->
        { req ->
            val method = req.message.method
            val rawParams = McpJson.fields(parse(req.http.bodyString())).toMap()["params"]

            val transportSpan = Span.current()

            val metaFields = rawParams
                ?.let { McpJson.fields(it).toMap()["_meta"] }
                ?.let { McpJson.fields(it).toMap() }
                ?: emptyMap()

            val parentContext = textMapPropagator.extract(Context.root(), metaFields, metaTextMapGetter)

            val targetName = rawParams
                ?.let { McpJson.fields(it).toMap()["name"] }
                ?.let { McpJson.text(it) }
            val spanName = if (targetName != null) "${method.value} $targetName" else method.value

            val span = tracer.spanBuilder(spanName)
                .setParent(parentContext)
                .setSpanKind(SERVER)
                .setAttribute("mcp.method.name", method.value)
                .setAttribute("mcp.session.id", req.session.id.value)
                .setAttribute("mcp.protocol.version", Header.MCP_PROTOCOL_VERSION(req.http).value)
                .apply {
                    req.message.id?.let { setAttribute("jsonrpc.request.id", it.toString()) }
                    if (transportSpan.spanContext.isValid) addLink(transportSpan.spanContext)
                }
                .startSpan()

            spanModifiers.forEach { it(span, req.message) }

            try {
                span.makeCurrent().use { next(req) }
                    .also { resp ->
                        if (resp is McpResponse.Ok) {
                            val message = resp.message
                            if (message is McpJsonRpcResponse) {
                                spanModifiers.forEach { it(span, message) }
                            }

                            if (resp.message is McpJsonRpcErrorResponse) {
                                span.setStatus(ERROR)
                                val code = McpJson.textValueOf(resp.message.error, "code")
                                if (code != null) span.setAttribute("error.type", code)
                            }
                        }
                    }
            } catch (e: Throwable) {
                span.setStatus(ERROR)
                span.setAttribute("error.type", e.javaClass.name)
                throw e
            } finally {
                span.end()
            }
        }
    }
}

val defaultMcpOtelSpanModifiers = listOf(
    CallToolSpanModifiers,
    CompletionSpanModifiers,
    GetPromptSpanModifiers,
    ReadResourceSpanModifiers
)

private val metaTextMapGetter = object : TextMapGetter<Map<String, McpNodeType>> {
    override fun keys(carrier: Map<String, McpNodeType>) = carrier.keys
    override fun get(carrier: Map<String, McpNodeType>?, key: String) =
        carrier?.get(key)?.let { McpJson.text(it) }
}

