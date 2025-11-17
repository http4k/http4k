package org.http4k.ai.mcp.conformance.server.tools

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Tool
import org.http4k.routing.bind

fun progressTool() = Tool("test_tool_with_progress", "test_tool_with_progress") bind {
    it.client.progress(0, 100.0, "Completed step 0 of 100")
    it.client.progress(50, 100.0, "Completed step 50 of 100")
    it.client.progress(100, 100.0, "Completed step 100 of 100")

    ToolResponse.Ok(textContent)
}
