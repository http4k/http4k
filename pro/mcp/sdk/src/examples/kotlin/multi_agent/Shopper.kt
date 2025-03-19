package multi_agent

import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.routing.bind
import org.http4k.routing.mcpSse

// TOOL create shopping list
// SERVER of NUTRITIONIST
// CLIENT of UI
val shopper = mcpSse(
    ServerMetaData("shopper", "1.0.0"),
    Tool(
        "create_shopping_list", "",
        Tool.Arg.required("ingredients")
    ) bind {
        // API LLM to create shopping list, get bad ingredients
        // SAMPLE nutritionist about healthy substitutions
        // API LLM to review and warn about alternatives (use date)
        // SAMPLE UI to adapt recipe for seasonality
        // API LLM to update shopping list
        ToolResponse.Ok(listOf())
    }
)

//NUTRITIONIST -> SHOPPER
//RECIPE -> NUTRITIONIST
//UI -> RECIPE
//UI -> SHOPPER
