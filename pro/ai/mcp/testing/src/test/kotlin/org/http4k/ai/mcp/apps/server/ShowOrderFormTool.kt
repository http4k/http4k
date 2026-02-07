package org.http4k.ai.mcp.apps.server

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.extension.CspDomain
import org.http4k.ai.mcp.model.extension.McpAppCsp
import org.http4k.ai.mcp.model.extension.McpAppMeta
import org.http4k.routing.bind

fun ShowOrderFormTool() = Tool(
    name = "show_order_form",
    description = "Display the order form UI",
    meta = Meta(
        ui = McpAppMeta(OrderFormUi.uri),
        csp = McpAppCsp(resourceDomains = listOf(CspDomain.of("unpkg.com")))
    )
) bind {
    ToolResponse.Ok(listOf(Content.Text("Opening order form...")))
}
