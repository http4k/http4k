package org.http4k.mcp.server.ws

import org.http4k.core.Request
import org.http4k.mcp.server.RealtimeMcpProtocol
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
            mcpProtocol.receive(newSessionId, req.body(it.bodyString()))
        }

        it.send(WsMessage(SseMessage.Event("endpoint", "").toMessage()))
    }
}
