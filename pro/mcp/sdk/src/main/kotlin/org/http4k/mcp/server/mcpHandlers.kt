package org.http4k.mcp.server

import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.format.jsonRpcRequest
import org.http4k.lens.Query
import org.http4k.lens.value
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.sse.RealtimeMcpProtocol
import org.http4k.mcp.util.McpJson
import org.http4k.routing.bind
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.routing.websocket.bind
import org.http4k.routing.websockets
import org.http4k.sse.SseMessage.Event
import org.http4k.websocket.WsMessage

/**
 * This is the main entry point for the MCP server. It sets up the SSE connection and then provides a
 * endpoint for the client to send messages to.
 */
fun McpSseHandler(mcpProtocol: RealtimeMcpProtocol) = poly(
    "/sse" bind sse {
        it.send(
            Event("endpoint", Request(GET, "/message").with(sessionId of mcpProtocol.newSession(it)).uri.toString())
        )
    },
    CatchLensFailure()
        .then(
            routes(
            "/message" bind POST to { req: Request ->
                mcpProtocol(sessionId(req), Body.jsonRpcRequest(McpJson).toLens()(req), req)
            }
        ))
)

/**
 * This is the main entry point for the MCP server.
 */
fun McpWsHandler(mcpProtocol: RealtimeMcpProtocol) = poly(
    "/ws" bind websockets {
        val newSessionId = mcpProtocol.newSession(it)
        it.onMessage {
            val req = Request(POST, "/ws").body(it.bodyString())
            mcpProtocol(newSessionId, Body.jsonRpcRequest(McpJson).toLens()(req), req)
        }

        it.send(WsMessage(Event("endpoint", "").toMessage()))
    }
)

private val sessionId = Query.value(SessionId).required("sessionId")
