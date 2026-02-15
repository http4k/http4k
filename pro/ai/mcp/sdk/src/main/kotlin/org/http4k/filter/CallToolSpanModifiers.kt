package org.http4k.filter

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType

object CallToolSpanModifiers : McpOpenTelemetrySpanModifiers {
    override val method = McpTool.Call.Method

    override fun request(sb: Span, request: McpNodeType) {
        sb.setAttribute("gen_ai.operation.name", "execute_tool")
        McpJson.fields(request).toMap()["name"]?.let {
            sb.setAttribute("gen_ai.tool.name", McpJson.text(it))
        }
    }

    override fun response(sb: Span, response: McpNodeType) {
        McpJson.fields(response).toMap()["isError"]?.let {
            sb.setStatus(StatusCode.ERROR)
            sb.setAttribute("error.type", "tool_error")
        }
    }
}
