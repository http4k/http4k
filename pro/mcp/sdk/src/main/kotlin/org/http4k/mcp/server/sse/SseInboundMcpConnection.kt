package org.http4k.mcp.server.sse

import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.mcp.server.protocol.InvalidSession
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.protocol.Session
import org.http4k.mcp.util.asHttp
import org.http4k.routing.bind
import org.http4k.sse.Sse

/**
 * Routes inbound POST requests to the MCP server to the MCP protocol for processing.
 */
fun SseInboundMcpConnection(protocol: McpProtocol<Sse>) =
    "/message" bind POST to { req ->
        when (val session = protocol.retrieveSession(req)) {
            is Session -> protocol.receive(protocol.transportFor(Subscription(session)), session, req).asHttp()
            InvalidSession -> Response(BAD_REQUEST)
        }
    }
