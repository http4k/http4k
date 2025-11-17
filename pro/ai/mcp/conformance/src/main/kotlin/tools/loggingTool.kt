package tools

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.LogLevel.info
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.util.McpJson
import org.http4k.routing.bind

fun loggingTool() =
    Tool("test_tool_with_logging", "test_tool_with_logging") bind {
        it.client.log(McpJson.string("Tool execution started"), info)
        it.client.log(McpJson.string("Tool processing data"), info)
        it.client.log(McpJson.string("Tool execution completed"), info)

        Ok(textContent)
    }

