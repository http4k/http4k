package org.http4k.mcp.server.sse

import org.http4k.core.Method.POST
import org.http4k.routing.routes

/**
 * This HTTP handler can be bound to whatever path is required by the server with
 * routes("/path" bind <SseMcpSessionCommandEndpoint>
 */
fun SseMcpSessionCommandEndpoint(transport: SseMcpTransport) =
    routes(POST to { transport.receive(sessionId(it), it) })

