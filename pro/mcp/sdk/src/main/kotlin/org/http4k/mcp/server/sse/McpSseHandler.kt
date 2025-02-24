package org.http4k.mcp.server.sse

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.mcp.server.RealtimeMcpProtocol
import org.http4k.routing.sse
import org.http4k.sse.SseMessage

/**
 * This SSE handler can be bound to whatever path is required by the server with
 * routes("/path" bind <McpSseHandler?
 */
fun McpSseHandler(mcpProtocol: RealtimeMcpProtocol) = sse {
    it.send(
        SseMessage.Event(
            "endpoint",
            Request(Method.GET, "/message").with(sessionId of mcpProtocol.newSession(it)).uri.toString()
        )
    )
}
