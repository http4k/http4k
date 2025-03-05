package org.http4k.mcp.server.http

import org.http4k.routing.sse

/**
 * This SSE handler can be bound to whatever path is required by the server with
 * routes("/path" bind <EventStreamCommandEndpoint>
 */
fun EventStreamCommandEndpoint(transport: EventStreamMcpTransport) = sse {
    val newSession = transport.newSession(it.connectRequest, it)

    transport.receive(newSession, it.connectRequest)
}
