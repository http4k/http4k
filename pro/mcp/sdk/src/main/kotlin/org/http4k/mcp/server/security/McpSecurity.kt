package org.http4k.mcp.server.security

import org.http4k.routing.RoutingHttpHandler
import org.http4k.security.Security

/**
 * Provides a way to secure an MCP server using various authentication methods.
 */
interface McpSecurity : Security {
    val routes: List<RoutingHttpHandler>
}
