package org.http4k.filter

import io.opentelemetry.api.trace.Span
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType

object ReadResourceSpanModifiers : McpOpenTelemetrySpanModifiers {
    override val method = McpResource.Read.Method

    override fun request(sb: Span, request: McpNodeType) {
        sb.setAttribute("gen_ai.operation.name", "read_resource")
        McpJson.fields(request).toMap()["uri"]?.let {
            sb.setAttribute("mcp.resource.uri", McpJson.text(it))
        }
    }
}
