package org.http4k.wiretap.mcp.prompts

import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction

fun Prompts(mcpClient: McpClient) = object : WiretapFunction {
    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) = "/prompts" bind routes(
        InspectPrompt(mcpClient, elements),
        CreatePrompt(mcpClient, elements),
        TabContent(mcpClient, elements)
    )

    override fun mcp() = CapabilityPack()
}
