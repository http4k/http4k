package org.http4k.mcp.server

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.format.AutoMarshallingJson
import org.http4k.format.jsonRpcRequest
import org.http4k.mcp.util.McpJson
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.SseMessage.Event
import org.http4k.routing.bind as httpBind

/**
 * This is the main entry point for the MCP server. It handles the various MCP messages on both HTTP and SSE.
 */
fun McpHandler(mcpProtocol: SseMcpProtocol, json: AutoMarshallingJson<JsonNode> = McpJson): PolyHandler {

    return poly(
        "/sse" bind sse {
            val sessionId = mcpProtocol.newSession(it)
            it.send(Event("endpoint", Uri.of("/message").query("sessionId", sessionId.value.toString()).toString()))
        },
        routes(
            "/message" httpBind POST to { req: Request ->
                mcpProtocol(SessionId.parse(req.query("sessionId")!!), Body.jsonRpcRequest(json).toLens()(req), req)
            }
        )
    )
}

