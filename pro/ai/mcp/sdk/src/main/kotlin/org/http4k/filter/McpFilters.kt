package org.http4k.filter

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import org.http4k.ai.mcp.server.protocol.McpFilter
import org.http4k.ai.mcp.util.McpJson
import org.http4k.metrics.Http4kOpenTelemetry

object McpFilters {
    fun OpenTelemetryTracing(openTelemetry: OpenTelemetry = GlobalOpenTelemetry.get()): McpFilter {
        val tracer = openTelemetry.tracerProvider.get(Http4kOpenTelemetry.INSTRUMENTATION_NAME)

        return McpFilter { next ->
            { req ->
                val span = tracer.spanBuilder(req.json.method)
                    .setParent(Context.current())
                    .setSpanKind(SpanKind.INTERNAL)
                    .setAttribute("mcp.method.name", req.json.method)
                    .setAttribute("mcp.session.id", req.session.id.value)
                    .apply {
                        req.json.id?.let { setAttribute("jsonrpc.request.id", McpJson.compact(it)) }
                    }
                    .startSpan()

                try {
                    span.makeCurrent().use { next(req) }
                        .also { resp ->
                            val error = McpJson.fields(resp.json).toMap()["error"]
                            if (error != null) {
                                span.setStatus(StatusCode.ERROR)
                                val code = McpJson.fields(error).toMap()["code"]
                                if (code != null) span.setAttribute("error.type", McpJson.compact(code))
                            }
                        }
                } catch (e: Throwable) {
                    span.setStatus(StatusCode.ERROR)
                    span.setAttribute("error.type", e.javaClass.name)
                    throw e
                } finally {
                    span.end()
                }
            }
        }
    }
}
