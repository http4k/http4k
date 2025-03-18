package `multi-agent`

import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.routing.bind
import org.http4k.routing.mcpSse
import org.http4k.server.Helidon
import org.http4k.server.asServer

val recipePlanner = mcpSse(
    ServerMetaData("recipe-planner", "1.0.0"),

    Tool("meal_request", "", Tool.Arg.required("instructions")) bind {
        // API recipe LLM to get the plan and ingredients
        // TOOL nutritionist to analyse nutrition
        // API recipe LLM to review the analysis
        // TOOL shopping to create shopping list, finding alternatives to BAD INGREDIENTS
        // API LLM to refine recipe
        // RETURN dinner plan with recipe, nutrition, and shopping list

        ToolResponse.Ok(listOf())
    },
)

val nutritionist = mcpSse(
    ServerMetaData("nutritionist", "1.0.0"),
    Tool(
        "analyze_nutrition", "",
        Tool.Arg.required("ingredients"),
    ) bind {
        // API LLM to get the basic analysis
        // SAMPLE client about serving sizes
        // API LLM to get the exact analysis with sizes

        ToolResponse.Ok(listOf())
    }
)

val shopper = mcpSse(
    ServerMetaData("shopper", "1.0.0"),
    Tool(
        "create_shopping_list", "",
        Tool.Arg.required("ingredients")
    ) bind {
        // API LLM to create shopping list, get bad ingredients
        // SAMPLE nutritionist about healthy substitutions
        // API LLM to review and warn about alternatives (use date)
        // SAMPLE client to adapt recipe for seasonality
        // API LLM to update shopping list
        ToolResponse.Ok(listOf())
    }
)

fun main() {
    recipePlanner.asServer(Helidon(30000)).start()
    nutritionist.asServer(Helidon(31000)).start()
    shopper.asServer(Helidon(31000)).start()
}
