/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter.stateless

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind.SERVER
import io.opentelemetry.api.trace.StatusCode.ERROR
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import org.http4k.ai.mcp.stateless.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.stateless.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.stateless.protocol.messages.McpPrompt
import org.http4k.ai.mcp.stateless.protocol.messages.McpTool
import org.http4k.ai.mcp.stateless.server.protocol.McpFilter
import org.http4k.ai.mcp.stateless.server.protocol.McpResponse
import org.http4k.ai.mcp.stateless.util.McpJson
import org.http4k.ai.mcp.stateless.util.McpNodeType
import org.http4k.ai.mcp.stateless.util.errorCode
import org.http4k.lens.Header
import org.http4k.lens.stateless.MCP_PROTOCOL_VERSION
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

            val span = tracer.spanBuilder(req.message.target()?.let { "$method $it" } ?: method.value)
                .setParent(
                    textMapPropagator.extract(
                        Context.root(),
                        req.message.params?._meta?.node?.attributes.orEmpty(), metaTextMapGetter
                    )
                )
                .setSpanKind(SERVER)
                .setAttribute("mcp.method.name", method.value)
                .setAttribute("mcp.protocol.version", Header.MCP_PROTOCOL_VERSION(req.http).value)
                .setAttribute("network.transport", "tcp")
                .setAttribute("network.protocol.name", "http")
                .apply {
                    req.message.id?.let { setAttribute("jsonrpc.request.id", it.toString()) }
                    req.http.source?.let {
                        setAttribute("client.address", it.address)
                        it.port?.let { port -> setAttribute("client.port", port.toLong()) }
                    }
                    if (Span.current().spanContext.isValid) addLink(Span.current().spanContext)
                }
                .startSpan()

            spanModifiers.forEach { it(span, req) }

            try {
                span.makeCurrent().use { next(req) }
                    .also { resp ->
                        spanModifiers.forEach { it(span, resp) }

                        if (resp is McpResponse.Ok && resp.message is McpJsonRpcErrorResponse) {
                            resp.message.error.errorCode()?.let { code ->
                                span.setAttribute("error.type", code.toString())
                                span.setAttribute("rpc.response.status_code", code.toString())
                                if (code !in CALLER_FAULT_CODES) span.setStatus(ERROR)
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

// the receiver could not serve what the caller sent; the semconv says these are not server errors
private val CALLER_FAULT_CODES = setOf(-32700, -32600, -32601, -32602, -32002)

val defaultMcpOtelSpanModifiers = listOf(
    CallToolSpanModifiers,
    GetPromptSpanModifiers,
    ReadResourceSpanModifiers
)

private fun McpJsonRpcRequest.target() = when (this) {
    is McpTool.Call.Request -> params.name.value
    is McpPrompt.Get.Request -> params.name.value
    else -> null
}

private val metaTextMapGetter = object : TextMapGetter<Map<String, McpNodeType>> {
    override fun keys(carrier: Map<String, McpNodeType>) = carrier.keys
    override fun get(carrier: Map<String, McpNodeType>?, key: String) =
        carrier?.get(key)?.let { McpJson.text(it) }
}
