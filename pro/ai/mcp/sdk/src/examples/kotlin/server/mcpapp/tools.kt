package server.mcpapp

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.int
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.routing.bind

fun showOrderFormTool() = Tool(
    name = "show_order_form",
    description = "Display the order form UI"
) bind {
    // The _meta.ui field tells the host to load the UI resource
    // For now, we just return text - the host will see the ui:// resource and render it
    Ok(listOf(Text("Opening order form...")), meta = Meta())
}

fun submitOrderTool(): ToolCapability {
    val product = Tool.Arg.string().required("product", "The product to order")
    val quantity = Tool.Arg.int().required("quantity", "How many to order")

    return Tool(
        name = "submit_order",
        description = "Submit an order for a product",
        product, quantity
    ) bind { args ->
        val productName = product(args)
        val qty = quantity(args)

        println("Order received: $qty x $productName")

        Ok(listOf(Text("Order confirmed: $qty x $productName")))
    }
}
