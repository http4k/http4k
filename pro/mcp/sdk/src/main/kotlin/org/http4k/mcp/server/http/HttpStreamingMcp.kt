package org.http4k.mcp.server.http

import org.http4k.core.then
import org.http4k.filter.CatchAllSse
import org.http4k.filter.ServerFilters
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.routing.poly
import org.http4k.sse.Sse
import org.http4k.sse.then

/**
 * Draft MCP server setup for Streaming HTTP-based MCP Servers which use HTTP + SSE
 *
 * NOTE THAT THIS IMPLEMENTATION IS BASED ON THE DRAFT MCP PROTOCOL AND IS SUBJECT TO CHANGE
 */
fun HttpStreamingMcp(mcpProtocol: McpProtocol<Sse>) = poly(
    ServerFilters.CatchAllSse().then(HttpStreamingMcpConnection(mcpProtocol)),
    CatchAll().then(CatchLensFailure()).then(HttpNonStreamingMcpConnection(mcpProtocol))
)

