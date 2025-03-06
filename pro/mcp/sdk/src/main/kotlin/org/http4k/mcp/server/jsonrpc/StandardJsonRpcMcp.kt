package org.http4k.mcp.server.jsonrpc

import org.http4k.core.Response
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.routing.bind
import org.http4k.routing.poly

/**
 * Standard MCP server setup for HTTP-based MCP Servers
 */
fun StandardJsonRpcMcp(protocol: McpProtocol<Unit, Response>) = poly(
    "/message" bind JsonRpcCommandEndpoint(protocol),
)
