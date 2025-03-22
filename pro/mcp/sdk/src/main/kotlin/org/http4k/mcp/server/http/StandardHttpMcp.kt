package org.http4k.mcp.server.http

import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Response
import org.http4k.core.accepted
import org.http4k.core.then
import org.http4k.filter.CatchAllSse
import org.http4k.filter.ServerFilters
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.routing.bind
import org.http4k.routing.bindSse
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.routing.sse
import org.http4k.sse.Sse
import org.http4k.sse.then

/**
 * Standard MCP server setup for Streaming HTTP-based MCP Servers
 */
fun StandardHttpMcp(mcpProtocol: McpProtocol<Sse, Response>) = poly(
    ServerFilters.CatchAllSse().then(
        "/mcp" bindSse sse(TEXT_EVENT_STREAM.accepted() bindSse HttpConnectionEndpoint(mcpProtocol))
    ),
    CatchAll().then(CatchLensFailure()).then(routes("/mcp" bind HttpCommandEndpoint(mcpProtocol)))
)
