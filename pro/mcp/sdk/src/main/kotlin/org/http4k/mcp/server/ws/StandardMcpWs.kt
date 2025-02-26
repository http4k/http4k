package org.http4k.mcp.server.ws

import org.http4k.mcp.server.RealtimeMcpProtocol
import org.http4k.routing.poly
import org.http4k.routing.websocket.bind
import org.http4k.websocket.Websocket

/**
 * Standard MCP server setup for WS-based MCP Servers
 */
fun StandardMcpWs(mcpProtocol: RealtimeMcpProtocol<Websocket>) = poly(
    "/ws" bind McpWsHandler(mcpProtocol)
)
