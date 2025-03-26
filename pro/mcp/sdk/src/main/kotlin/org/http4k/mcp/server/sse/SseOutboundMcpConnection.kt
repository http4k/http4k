package org.http4k.mcp.server.sse

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.mcp.server.protocol.ClientRequestContext.Stream
import org.http4k.mcp.server.protocol.InvalidSession
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.protocol.Session
import org.http4k.routing.sse.bind
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import org.http4k.sse.SseResponse

/**
 * Persistent SSE which routes outbound MCP messages for the allocated session to the client
 */
fun SseOutboundMcpConnection(protocol: McpProtocol<Sse>) =
    "/sse" bind { req: Request ->
        when (val session = protocol.retrieveSession(req)) {
            is Session -> SseResponse(OK) {
                protocol.assign(Stream(session), it, req)
                it.send(
                    SseMessage.Event(
                        "endpoint",
                        Request(GET, "/message").with(sessionId of session.id).uri.toString()
                    )
                )
            }

            is InvalidSession -> SseResponse(NOT_FOUND) {
                it.close()
            }
        }
    }
