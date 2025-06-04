package org.http4k.ai.mcp.server.http

import org.http4k.core.HttpFilter
import org.http4k.core.then
import org.http4k.filter.CatchAllSse
import org.http4k.filter.ServerFilters
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.security.McpSecurity
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.sse.Sse
import org.http4k.sse.SseFilter
import org.http4k.sse.then

/**
 * MCP server setup for Streaming HTTP-based MCP Servers which use HTTP + SSE
 */
fun HttpStreamingMcp(mcpProtocol: McpProtocol<Sse>, security: McpSecurity, path: String = "/mcp") = poly(
    ServerFilters.CatchAllSse().then(SseFilter(security).then(HttpStreamingMcpConnection(mcpProtocol, path))),
    CatchAll()
        .then(CatchLensFailure())
        .then(routes(security.routes + HttpFilter(security).then(
            org.http4k.ai.mcp.server.http.HttpNonStreamingMcpConnection(
                mcpProtocol,
                path
            )
        )))
)


