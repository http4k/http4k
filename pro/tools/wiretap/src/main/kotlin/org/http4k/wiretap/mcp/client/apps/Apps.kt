package org.http4k.wiretap.mcp.client.apps

import org.http4k.ai.mcp.apps.McpApps
import org.http4k.ai.mcp.apps.endpoints.ReadUiResource
import org.http4k.ai.mcp.apps.endpoints.ToolCall
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction

fun Apps(mcpClient: McpClient) = object : WiretapFunction {
    override fun http(
        elements: DatastarElementRenderer,
        html: TemplateRenderer
    ): RoutingHttpHandler {
        val mcpApps = McpApps(listOf(mcpClient)).apply { runCatching { start() } }
        return routes(
            ReadUiResource(mcpApps),
            ToolCall(mcpApps),
            Index(html, mcpApps.tools())
        )
    }

    override fun mcp() = CapabilityPack()
}
