package org.http4k.mcp.server.sse

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.with
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.routing.sse
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage

/**
 * This SSE connection handler can be bound to whatever path is required by the server with
 * routes("/path" bind <SseConnectionHandle>
 */
fun SseConnectionEndpoint(protocol: McpProtocol<Sse, Response>) = sse {
    it.send(
        SseMessage.Event(
            "endpoint",
            Request(GET, "/message").with(sessionId of protocol.newSession(it.connectRequest, it)).uri.toString()
        )
    )
}
