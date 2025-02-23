package org.http4k.mcp.server.ws

import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.format.jsonRpcRequest
import org.http4k.mcp.server.RealtimeMcpProtocol
import org.http4k.mcp.util.McpJson
import org.http4k.routing.websockets
import org.http4k.sse.SseMessage
import org.http4k.websocket.WsMessage

/**
 * This Websocket handler can be bound to whatever path is required by the server with
 * ws("/path" bind <McpWsHandler>
 */
fun McpWsHandler(mcpProtocol: RealtimeMcpProtocol) = websockets {
    val newSessionId = mcpProtocol.newSession(it)
    it.onMessage {
        val req = Request(POST, "/ws").body(it.bodyString())
        mcpProtocol(newSessionId, Body.jsonRpcRequest(McpJson).toLens()(req), req)
    }

    it.send(WsMessage(SseMessage.Event("endpoint", "").toMessage()))
}
