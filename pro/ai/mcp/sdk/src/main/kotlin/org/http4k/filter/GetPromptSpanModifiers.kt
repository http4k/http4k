package org.http4k.filter

import io.opentelemetry.api.trace.Span
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType

object GetPromptSpanModifiers : McpOpenTelemetrySpanModifiers {
    override val method = McpPrompt.Get.Method
    override fun request(sb: Span, request: McpNodeType) {
        sb.setAttribute("gen_ai.operation.name", "get_prompt")
        McpJson.fields(request).toMap()["name"]?.let {
            sb.setAttribute("gen_ai.prompt.name", McpJson.text(it))
        }
    }
}
