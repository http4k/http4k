package org.http4k.mcp.server.ws

import org.http4k.routing.poly
import org.http4k.routing.websocket.bind

/**
 * Standard MCP server setup for WS-based MCP Servers
 */
fun StandardWsMcp(session: WsMcpSession) = poly(
    "/ws" bind WsCommandHandler(session)
)
