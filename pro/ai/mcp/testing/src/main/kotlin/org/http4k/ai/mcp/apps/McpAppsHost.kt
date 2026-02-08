package org.http4k.ai.mcp.apps

import org.http4k.ai.mcp.apps.endpoints.ReadUiResource
import org.http4k.ai.mcp.apps.endpoints.ToolCall
import org.http4k.ai.mcp.apps.endpoints.Ui
import org.http4k.ai.mcp.testing.McpClientFactory
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.routes

/**
 * Basic MCP App host.
 */
fun McpAppsHost(vararg clients: McpClientFactory): RoutingHttpHandler {
    val servers = McpApps(clients.map { it() }).apply { start() }
    return CatchLensFailure()
        .then(
            routes(
                ReadUiResource(servers),
                ToolCall(servers),
                Ui(servers)
            )
        )
}
