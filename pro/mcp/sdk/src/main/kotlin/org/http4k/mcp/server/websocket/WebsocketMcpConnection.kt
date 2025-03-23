package org.http4k.mcp.server.websocket

import org.http4k.core.Request
import org.http4k.mcp.server.protocol.InvalidSession
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.protocol.Session
import org.http4k.routing.bindWs
import org.http4k.sse.SseMessage
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus.Companion.REFUSE

/**
 * This Websocket handler can be bound to whatever path is required by the server with
 * ws("/path" bind <WsCommandHandler>
 */
fun WebsocketMcpConnection(protocol: McpProtocol<Websocket, Unit>) = "/ws" bindWs { req: Request ->
    when (val session = protocol.retrieveSession(req)) {
        is Session -> WsResponse { ws ->
            with(protocol) {
                assign(session, ws, req)
                ws.onMessage { receive(ws, session, req.body(it.bodyString())) }
                ws.send(WsMessage(SseMessage.Event("endpoint", "").toMessage()))
            }
        }

        is InvalidSession -> WsResponse { it.close(REFUSE) }
    }
}
