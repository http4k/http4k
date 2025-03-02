package org.http4k.mcp.server.ws

import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.format.jsonRpcRequest
import org.http4k.mcp.server.RealtimeMcpProtocol
import org.http4k.mcp.util.McpJson
import org.http4k.sse.SseMessage
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse

/**
 * This Websocket handler can be bound to whatever path is required by the server with
 * ws("/path" bind <McpWsHandler>
 */
fun McpWsHandler(mcpProtocol: RealtimeMcpProtocol<Websocket>) = { req: Request ->
    WsResponse {
        val newSessionId = mcpProtocol.newSession(req, it)
        it.onMessage {
            val newReq = Request(POST, "/ws").body(it.bodyString())
            mcpProtocol(newSessionId, Body.jsonRpcRequest(McpJson).toLens()(newReq), req.body(it.bodyString()))
        }

        it.send(WsMessage(SseMessage.Event("endpoint", "").toMessage()))
    }
}
