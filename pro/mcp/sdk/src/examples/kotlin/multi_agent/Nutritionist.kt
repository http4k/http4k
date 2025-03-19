package multi_agent

import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.routing.bind
import org.http4k.routing.mcpSse

// TOOL analyse nutrition
// SERVER of UI
val nutritionist = mcpSse(
    ServerMetaData("nutritionist", "1.0.0"),
    Tool(
        "analyze_nutrition", "",
        Tool.Arg.required("ingredients"),
    ) bind {
        // API LLM to get the basic analysis
        // SAMPLE UI about serving sizes
        // API LLM to get the exact analysis with sizes

        ToolResponse.Ok(listOf())
    }
)
