package org.http4k.mcp.server.ws

import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.routing.poly
import org.http4k.routing.websocket.bind
import org.http4k.websocket.Websocket

/**
 * Standard MCP server setup for WS-based MCP Servers
 */
fun StandardWsMcp(mcpProtocol: McpProtocol<Websocket, Unit>) = poly(
    "/ws" bind WsCommandEndpoint(mcpProtocol)
)
