package org.http4k.mcp.server.http

import org.http4k.core.Response
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
 * Standard MCP server setup for Streaming HTTP-based MCP Servers
 */
fun StandardHttpMcp(mcpProtocol: McpProtocol<Sse, Response>) = poly(
    ServerFilters.CatchAllSse().then(HttpStreamingConnection(mcpProtocol)),
    CatchAll().then(CatchLensFailure()).then(HttpNonStreamingConnection(mcpProtocol))
)
