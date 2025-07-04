@file:JvmName("StandardSseMcpKt")

package org.http4k.ai.mcp.server.sse

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
 * Standard MCP server setup for SSE-based MCP Servers
 */
fun SseMcp(mcpProtocol: McpProtocol<Sse>, security: McpSecurity) =
    poly(
        ServerFilters.CatchAllSse().then(SseFilter(security).then(SseOutboundMcpConnection(mcpProtocol))),
        CatchAll().then(CatchLensFailure())
            .then(routes(security.routes + HttpFilter(security).then(SseInboundMcpConnection(mcpProtocol)))),
    )
