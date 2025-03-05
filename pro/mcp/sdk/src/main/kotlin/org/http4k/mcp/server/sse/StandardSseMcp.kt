package org.http4k.mcp.server.sse

import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.routing.bind
import org.http4k.routing.poly
import org.http4k.routing.sse.bind

/**
 * Standard MCP server setup for SSE-based MCP Servers
 */
fun StandardSseMcp(transport: SseMcpTransport) = poly(
    "/sse" bind SseMcpSessionConnectionEndpoint(transport),
    CatchLensFailure().then("/message" bind SseMcpSessionCommandEndpoint(transport))
)
