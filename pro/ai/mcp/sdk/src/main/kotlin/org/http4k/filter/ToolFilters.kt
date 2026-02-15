package org.http4k.filter

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode.ERROR
import org.http4k.ai.mcp.ToolFilter
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.model.ToolName

object ToolFilters {
    fun OpenTelemetryTracing(toolName: ToolName) = ToolFilter { next ->
        {
            Span.current().apply {
                setAttribute("gen_ai.tool.name", toolName.value)
                setAttribute("gen_ai.operation.name", "execute_tool")
            }
            next(it).also { response ->
                if (response is ToolResponse.Error) {
                    Span.current().apply {
                        setStatus(ERROR)
                        setAttribute("error.type", "tool_error")
                    }
                }
            }
        }
    }
}
