package org.http4k.ai.mcp.apps.server

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Tool
import org.http4k.routing.bind

fun GetShoppingList(list: MutableMap<String, String>) = Tool(
    name = "get_shopping_list",
    description = "View the current shopping list",
) bind {
    ToolResponse.Ok(listOf(Content.Text(list.entries.joinToString("\n") { (product, qty) -> "$qty x $product" })))
}
