package tools

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Tool
import org.http4k.routing.bind

val textContent = Text("This is a simple text response for testing.")

fun simpleTextTool() = Tool("test_simple_text", "test_simple_text") bind { Ok(textContent) }

