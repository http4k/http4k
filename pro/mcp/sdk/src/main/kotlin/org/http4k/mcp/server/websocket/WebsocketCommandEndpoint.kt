package org.http4k.mcp.server.websocket

import org.http4k.core.Request
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsResponse

/**
 * This Websocket handler can be bound to whatever path is required by the server with
 * ws("/path" bind <WsCommandHandler>
 */
fun WebsocketCommandEndpoint(mcpProtocol: McpProtocol<Websocket, Unit>) = { req: Request ->
    WsResponse {
        val newSessionId = mcpProtocol.newSession(req, it)
        it.onMessage { mcpProtocol.receive(newSessionId, req.body(it.bodyString())) }
    }
}
