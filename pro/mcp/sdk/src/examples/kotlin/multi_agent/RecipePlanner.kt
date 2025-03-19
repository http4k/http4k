package multi_agent

import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.routing.bind
import org.http4k.routing.mcpSse

// TOOL meal request
// CLIENT of NUTRITIONIST
// CLIENT of SHOPPER
// PROMPT instructions
// COMPLETION recipe list

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
