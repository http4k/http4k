package org.http4k.ai.mcp.server.websocket

import org.http4k.filter.CatchAllWs
import org.http4k.filter.ServerFilters
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.security.McpSecurity
import org.http4k.routing.poly
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsFilter
import org.http4k.websocket.then

/**
 * Non-standard (but compliant) MCP server setup for WS-based MCP Servers
 */
fun WebsocketMcp(mcpProtocol: McpProtocol<Websocket>, security: McpSecurity) =
    poly(
        security.routes +
            ServerFilters.CatchAllWs().then(
                WsFilter(security).then(WebsocketMcpConnection(mcpProtocol))
            )
    )

