package org.http4k.mcp.server.sse

import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.format.jsonRpcRequest
import org.http4k.mcp.server.RealtimeMcpProtocol
import org.http4k.mcp.util.McpJson
import org.http4k.routing.routes

/**
 * This HTTP handler can be bound to whatever path is required by the server with
 * routes("/path" bind <McpHttpHandler>
 */
fun McpHttpHandler(mcpProtocol: RealtimeMcpProtocol<*>) =
    routes(POST to { req ->
        mcpProtocol(sessionId(req), Body.jsonRpcRequest(McpJson).toLens()(req), req)
    })

