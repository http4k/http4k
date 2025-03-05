package org.http4k.mcp.server.http

import org.http4k.routing.poly
import org.http4k.routing.sse.bind

/**
 * Standard MCP server setup for Pure SSE-based MCP Servers
 */
fun StandardHttpMcp(session: EventStreamMcpSession) = poly(
    "/sse" bind EventStreamCommandEndpoint(session),
    // TODO insert handler here for messages
)
