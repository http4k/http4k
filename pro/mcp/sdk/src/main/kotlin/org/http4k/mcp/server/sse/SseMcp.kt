@file:JvmName("StandardSseMcpKt")

package org.http4k.mcp.server.sse

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
 * Standard MCP server setup for SSE-based MCP Servers
 */
fun SseMcp(mcpProtocol: McpProtocol<Sse>) = poly(
    ServerFilters.CatchAllSse().then(SseOutboundMcpConnection(mcpProtocol)),
    CatchAll().then(CatchLensFailure()).then(SseInboundMcpConnection(mcpProtocol))
)
