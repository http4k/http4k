package org.http4k.wiretap.mcp.apps

import org.http4k.ai.mcp.apps.McpApps
import org.http4k.ai.mcp.apps.endpoints.ReadUiResource
import org.http4k.ai.mcp.apps.endpoints.ToolCall
import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction

fun Apps(mcpApps: McpApps) = object : WiretapFunction {
    override fun http(
        elements: DatastarElementRenderer,
        html: TemplateRenderer
    ): RoutingHttpHandler = "/apps" bind routes(
        ReadUiResource(mcpApps),
        ToolCall(mcpApps),
        TabContent(mcpApps.tools(), elements),
    )

    override fun mcp() = CapabilityPack()
}
