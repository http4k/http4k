package org.http4k.wiretap.mcp

import org.http4k.ai.mcp.apps.McpApps
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.mcp.apps.TabContent
import org.http4k.wiretap.mcp.apps.Apps
import org.http4k.wiretap.mcp.prompts.Prompts
import org.http4k.wiretap.mcp.resources.Resources
import org.http4k.wiretap.mcp.tools.Tools
import org.http4k.wiretap.mcp.prompts.TabContent

fun InboundClient(mcpClient: McpClient) = object : WiretapFunction {
    private val functions = listOf(Tools(mcpClient), Prompts(mcpClient), Resources(mcpClient))

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer): RoutingHttpHandler {
        val mcpApps = McpApps(listOf(mcpClient)).apply { runCatching { start() } }

        return routes(
            Index(html),
            org.http4k.wiretap.mcp.tools.TabContent(mcpClient, elements),
            TabContent(mcpClient, elements),
            org.http4k.wiretap.mcp.resources.TabContent(mcpClient, elements),
            TabContent(mcpApps.tools(), elements),
            *functions.map { it.http(elements, html) }.toTypedArray(),
            "/apps" bind Apps(mcpApps).http(elements, html)
        )
    }

    override fun mcp() = CapabilityPack()
}
