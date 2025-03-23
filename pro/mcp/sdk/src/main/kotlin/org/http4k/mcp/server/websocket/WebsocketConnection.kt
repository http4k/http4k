package org.http4k.mcp.server.websocket

import org.http4k.core.Request
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.protocol.Session.Invalid
import org.http4k.mcp.server.protocol.Session.Valid
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
fun WebsocketConnection(protocol: McpProtocol<Websocket, Unit>) = "/ws" bindWs { req: Request ->
    when (val session = protocol.validate(req)) {
        is Valid -> WsResponse { ws ->
            with(protocol) {
                assign(session, ws)
                ws.onMessage { receive(ws, session.sessionId, req.body(it.bodyString())) }
                ws.send(WsMessage(SseMessage.Event("endpoint", "").toMessage()))
            }
        }

        is Invalid -> WsResponse { it.close(REFUSE) }
    }
}
