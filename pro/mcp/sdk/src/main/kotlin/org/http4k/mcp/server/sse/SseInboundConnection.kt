package org.http4k.mcp.server.sse

import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.protocol.Session.Valid.Existing
import org.http4k.routing.bind
import org.http4k.sse.Sse

/**
 * Routes inbound POST requests to the MCP server to the MCP protocol for processing.
 */
fun SseInboundConnection(protocol: McpProtocol<Sse, Response>) =
    "/message" bind POST to { req ->
        when (val session = protocol.validate(req)) {
            is Existing -> protocol.receive(protocol.transportFor(session), session.sessionId, req)
            else -> Response(BAD_REQUEST)
        }
    }
