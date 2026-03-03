package org.http4k.wiretap.mcp.client.resources

import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction

fun Resources(mcpClient: McpClient) = object : WiretapFunction {
    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) = routes(
        InspectResource(mcpClient, elements),
        InspectTemplate(mcpClient, elements),
        ReadResource(mcpClient, elements)
    )

    override fun mcp() = CapabilityPack()
}
