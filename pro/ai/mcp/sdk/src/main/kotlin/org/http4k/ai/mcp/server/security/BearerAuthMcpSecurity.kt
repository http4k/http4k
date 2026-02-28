package org.http4k.ai.mcp.server.security

import org.http4k.filter.ServerFilters
import org.http4k.routing.RoutingHttpHandler

/**
 * Bearer token authentication for MCP servers.
 */
class BearerAuthMcpSecurity(checkToken: (String) -> Boolean) : McpSecurity {
    override val name = "Bearer Auth"

    override val routes = emptyList<RoutingHttpHandler>()
    override val filter = ServerFilters.BearerAuth(checkToken)
}
