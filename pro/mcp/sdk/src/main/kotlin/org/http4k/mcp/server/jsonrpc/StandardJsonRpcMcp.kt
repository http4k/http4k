package org.http4k.mcp.server.jsonrpc

import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.sse.SseCommandEndpoint
import org.http4k.routing.bind
import org.http4k.routing.poly

/**
 * Standard MCP server setup for HTTP-based MCP Servers
 */
fun StandardJsonRpcMcp(protocol: McpProtocol<Unit, Response>) = poly(
    CatchAll().then(CatchLensFailure()).then("/jsonrpc" bind JsonRpcCommandEndpoint(protocol))
)
