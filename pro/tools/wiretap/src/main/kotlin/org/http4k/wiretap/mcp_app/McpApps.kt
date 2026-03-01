package org.http4k.wiretap.mcp_app

import org.http4k.ai.mcp.apps.McpApps
import org.http4k.ai.mcp.apps.endpoints.ReadUiResource
import org.http4k.ai.mcp.apps.endpoints.ToolCall
import org.http4k.ai.mcp.client.http.HttpNonStreamingMcpClient
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.filter.debug
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction

class McpApps(uri: Uri, client: HttpHandler) : WiretapFunction {
    private val mcpApps = McpApps(
        listOf(
            HttpNonStreamingMcpClient(
                McpEntity.of("http4k Wiretap"), Version.of("0.0.0"),
                uri.path("/mcp"), client
            )
                .apply { start() })
    ).apply { start() }

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "/mcp-app" bind routes(
            ReadUiResource(mcpApps),
            ToolCall(mcpApps),
            Index(html)
        )

    override fun mcp() = CapabilityPack()
}
