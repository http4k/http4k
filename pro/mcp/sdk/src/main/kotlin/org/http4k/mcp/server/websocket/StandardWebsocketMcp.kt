package org.http4k.mcp.server.websocket

import org.http4k.filter.CatchAllWs
import org.http4k.filter.ServerFilters
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.routing.poly
import org.http4k.routing.websocket.bind
import org.http4k.websocket.Websocket
import org.http4k.websocket.then

/**
 * Standard MCP server setup for WS-based MCP Servers
 */
fun StandardWebsocketMcp(mcpProtocol: McpProtocol<Websocket, Unit>) = poly(
    ServerFilters.CatchAllWs().then("/ws" bind WebsocketCommandEndpoint(mcpProtocol)),
)
