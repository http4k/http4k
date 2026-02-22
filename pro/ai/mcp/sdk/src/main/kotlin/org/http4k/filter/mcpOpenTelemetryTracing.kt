package org.http4k.filter

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind.SERVER
import io.opentelemetry.api.trace.StatusCode.ERROR
import io.opentelemetry.context.Context
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.server.protocol.McpFilter
import org.http4k.ai.mcp.util.McpJson
import org.http4k.lens.Header
import org.http4k.lens.MCP_PROTOCOL_VERSION
import org.http4k.metrics.Http4kOpenTelemetry.INSTRUMENTATION_NAME

/**
 * OpenTelemetry tracing for MCP servers. Follows the latest conventions from the OTel spec.
 */
fun McpFilters.OpenTelemetryTracing(
    spanModifiers: List<McpOpenTelemetrySpanModifiers> = defaultMcpOtelSpanModifiers,
    openTelemetry: OpenTelemetry = GlobalOpenTelemetry.get()
): McpFilter {
    val tracer = openTelemetry.tracerProvider.get(INSTRUMENTATION_NAME)

    val spanModifierMap = spanModifiers.associateBy { it.method }

    return McpFilter { next ->
        { req ->
            val spanModifiers = spanModifierMap[McpRpcMethod.of(req.json.method)]

            val transportSpan = Span.current()

            val span = tracer.spanBuilder(req.json.method)
                .setParent(Context.current())
                .setSpanKind(SERVER)
                .setAttribute("mcp.method.name", req.json.method)
                .setAttribute("mcp.session.id", req.session.id.value)
                .setAttribute("mcp.protocol.version", Header.MCP_PROTOCOL_VERSION(req.http).value)
                .apply {
                    req.json.id?.let { setAttribute("jsonrpc.request.id", McpJson.compact(it)) }
                    if (transportSpan.spanContext.isValid) addLink(transportSpan.spanContext)
                }
                .startSpan()

            spanModifiers?.request(span, req.json.params ?: McpJson.obj())

            try {
                span.makeCurrent().use { next(req) }
                    .also { resp ->
                        spanModifiers?.response(span, resp.json)

                        val error = McpJson.fields(resp.json).toMap()["error"]
                        if (error != null) {
                            span.setStatus(ERROR)
                            val code = McpJson.fields(error).toMap()["code"]
                            if (code != null) span.setAttribute("error.type", McpJson.compact(code))
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
    GetPromptSpanModifiers,
    ReadResourceSpanModifiers
)

