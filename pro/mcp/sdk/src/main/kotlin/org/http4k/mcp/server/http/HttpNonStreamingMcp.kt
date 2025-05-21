package org.http4k.mcp.server.http

import org.http4k.core.HttpFilter
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.security.McpSecurity
import org.http4k.routing.routes
import org.http4k.sse.Sse

/**
 * MCP server setup for Non-streaming HTTP-based MCP Servers
 */
fun HttpNonStreamingMcp(mcpProtocol: McpProtocol<Sse>, security: McpSecurity, path: String = "/mcp") =
    CatchAll()
        .then(CatchLensFailure())
        .then(routes(security.routes + HttpFilter(security).then(HttpNonStreamingMcpConnection(mcpProtocol, path))))

