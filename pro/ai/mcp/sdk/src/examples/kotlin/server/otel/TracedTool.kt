package server.otel

import org.http4k.ai.mcp.ToolFilter
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.ai.mcp.server.capability.then
import org.http4k.ai.model.ToolName
import org.http4k.routing.bind

fun TracedTool(filter: (ToolName) -> ToolFilter): ToolCapability {
    val input = Tool.Arg.string().required("name")
    val toolName = ToolName.of("reverse")

    return filter(toolName).then(
        Tool(toolName.value, "description", input) bind {
            ToolResponse.Ok(listOf(Content.Text(input(it).reversed())))
        }
    )
}
