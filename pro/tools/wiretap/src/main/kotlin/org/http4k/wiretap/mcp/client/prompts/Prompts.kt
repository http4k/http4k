package org.http4k.wiretap.mcp.client.prompts

import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction

fun Prompts(mcpClient: McpClient) = object : WiretapFunction {
    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) = routes(
        InspectPrompt(mcpClient, elements),
        GetPrompt(mcpClient, elements)
    )

    override fun mcp() = CapabilityPack()
}
