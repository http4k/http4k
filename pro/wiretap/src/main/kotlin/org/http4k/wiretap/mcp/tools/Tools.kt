package org.http4k.wiretap.mcp.tools

import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction

fun Tools(mcpClient: McpClient) = object : WiretapFunction {
    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) = "/tools" bind routes(
        InspectTool(mcpClient, elements),
        CallTool(mcpClient, elements),
        TabContent(mcpClient, elements)
    )

    override fun mcp() = CapabilityPack()
}
