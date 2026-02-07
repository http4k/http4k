package org.http4k.ai.mcp.apps.server

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.string
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.routing.bind

fun SubmitOrderTool(): ToolCapability {
    val quantity = Tool.Arg.string().required("quantity", "How many to order")

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

val product = Tool.Arg.string().required("product", "The product to order")
