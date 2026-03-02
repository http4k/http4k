package org.http4k.wiretap.mcp.client

import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.mcp.client.prompts.Prompts
import org.http4k.wiretap.mcp.client.resources.Resources
import org.http4k.wiretap.mcp.client.tools.Tools

fun InboundClient(mcpClient: McpClient) = object : WiretapFunction {
    private val functions = listOf(Tools(mcpClient), Prompts(mcpClient), Resources(mcpClient))

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "/inbound" bind routes(functions.map { it.http(elements, html) })

    override fun mcp() = CapabilityPack()
}
