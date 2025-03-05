package org.http4k.mcp.server.http

import org.http4k.core.Response
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.routing.poly
import org.http4k.routing.sse.bind
import org.http4k.sse.Sse

/**
 * Standard MCP server setup for Pure SSE-based MCP Servers
 */
fun StandardHttpMcp(mcpProtocol: McpProtocol<Response, Sse>) = poly(
    "/sse" bind EventStreamCommandEndpoint(mcpProtocol),
    // TODO insert handler here for messages
)
