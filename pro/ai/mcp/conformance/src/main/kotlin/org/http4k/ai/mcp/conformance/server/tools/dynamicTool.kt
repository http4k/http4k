package org.http4k.ai.mcp.conformance.server.tools

import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.model.Tool
import org.http4k.routing.bind

fun dynamicTool() = Tool("test_dynamic_tool", "test_dynamic_tool") bind {
    Error("Not implemented yet")
}
