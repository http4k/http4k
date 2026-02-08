package org.http4k.ai.mcp.apps.server

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.extension.McpAppMeta
import org.http4k.routing.bind

fun ShowOrderFormTool() = Tool(
    name = "show_order_form",
    description = "Display the order form UI",
    meta = Meta(ui = McpAppMeta(OrderFormUi.uri))
) bind {
    ToolResponse.Ok(listOf(Text("Opening order form...")))
}
