package org.http4k.ai.mcp.apps

import org.http4k.ai.mcp.apps.endpoints.ReadUiResource
import org.http4k.ai.mcp.apps.endpoints.ToolCall
import org.http4k.ai.mcp.apps.endpoints.Ui
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.routes

fun McpAppsHost(servers: List<Uri>, http: HttpHandler = JavaHttpClient()): RoutingHttpHandler {
    val servers = ConnectedMcpServers(servers, http)
    return CatchLensFailure()
        .then(
            routes(
                ReadUiResource(servers),
                ToolCall(servers),
                Ui(servers)
            )
        )
}
