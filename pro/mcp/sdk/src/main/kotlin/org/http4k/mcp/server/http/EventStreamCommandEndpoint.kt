package org.http4k.mcp.server.http

import org.http4k.routing.sse

/**
 * This SSE handler can be bound to whatever path is required by the server with
 * routes("/path" bind <EventStreamCommandEndpoint>
 */
fun EventStreamCommandEndpoint(session: EventStreamMcpSession) = sse {
    val newSession = session.new(it.connectRequest, it)

    session.receive(newSession, it.connectRequest)
}
