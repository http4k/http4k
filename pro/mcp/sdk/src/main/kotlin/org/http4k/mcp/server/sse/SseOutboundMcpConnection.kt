package org.http4k.mcp.server.sse

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.sessions.Session.Invalid
import org.http4k.mcp.server.sessions.Session.Valid
import org.http4k.routing.sse.bind
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import org.http4k.sse.SseResponse

/**
 * Persistent SSE which routes outbound MCP messages for the allocated session to the client
 */
fun SseOutboundMcpConnection(protocol: McpProtocol<Sse, Response>) =
    "/sse" bind { req: Request ->
        when (val session = protocol.validate(req)) {
            is Valid -> SseResponse(OK) {
                protocol.assign(session, it, req)
                it.send(
                    SseMessage.Event(
                        "endpoint",
                        Request(GET, "/message").with(sessionId of session.sessionId).uri.toString()
                    )
                )
            }

            is Invalid -> SseResponse(BAD_REQUEST) {
                it.close()
            }
        }
    }
