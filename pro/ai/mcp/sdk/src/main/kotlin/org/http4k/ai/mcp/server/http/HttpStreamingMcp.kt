package org.http4k.ai.mcp.server.http

import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.security.McpSecurity
import org.http4k.core.HttpFilter
import org.http4k.core.then
import org.http4k.filter.PolyFilters
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.sse.Sse
import org.http4k.sse.SseFilter
import org.http4k.sse.then

/**
 * MCP server setup for Streaming HTTP-based MCP Servers which use HTTP + SSE
 */
fun HttpStreamingMcp(mcpProtocol: McpProtocol<Sse>, security: McpSecurity, path: String = "/mcp") =
    PolyFilters.CatchAll().then(
        poly(
            SseFilter(security).then(HttpStreamingMcpConnection(mcpProtocol, path)),
            CatchLensFailure()
                .then(
                    routes(
                        security.routes + HttpFilter(security).then(
                            HttpNonStreamingMcpConnection(
                                mcpProtocol,
                                path
                            )
                        )
                    )
                )
        )
    )


