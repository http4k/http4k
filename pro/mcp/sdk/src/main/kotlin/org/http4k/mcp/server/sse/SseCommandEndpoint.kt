package org.http4k.mcp.server.sse

import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.routing.routes

/**
 * This HTTP handler can be bound to whatever path is required by the server with
 * routes("/path" bind <SseCommandHandler>
 */
fun SseCommandEndpoint(mcpProtocol: McpProtocol<*, Response>) =
    routes(POST to { mcpProtocol.receive(sessionId(it), it) })

