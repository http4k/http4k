package tools

import org.http4k.ai.mcp.model.Tool
import org.http4k.routing.bind

fun errorHandlingTool() = Tool("test_error_handling", "test_error_handling") bind {
//    ToolResponse.Error(1, "This tool intentionally returns an error for testing")
    throw Exception("This tool intentionally returns an error for testing")
}

