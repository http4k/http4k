package org.http4k.ai.mcp.apps

import org.http4k.ai.mcp.apps.endpoints.ReadUiResource
import org.http4k.ai.mcp.apps.endpoints.ToolCall
import org.http4k.ai.mcp.apps.endpoints.Ui
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.routes

data class McpAppsServer(val uri: Uri)

fun McpAppsHost(servers: List<Uri>, clientFactory: McpClientFactory = McpClientFactory.Http()): RoutingHttpHandler {
    val servers = ConnectedMcpServers(servers, clientFactory).apply { start() }
    return CatchLensFailure()
        .then(
            routes(
                ReadUiResource(servers),
                ToolCall(servers),
                Ui(servers)
            )
        )
}
