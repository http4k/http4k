package org.http4k.ai.mcp.server.websocket

import org.http4k.core.Request
import org.http4k.ai.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.ai.mcp.server.protocol.InvalidSession
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.routing.bindWs
import org.http4k.sse.SseMessage
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus.Companion.REFUSE
import java.util.concurrent.Executors

/**
 * This Websocket handler can be bound to whatever path is required by the server with
 * ws("/path" bind <WsCommandHandler>
 */
fun WebsocketMcpConnection(protocol: McpProtocol<Websocket>) = "/ws" bindWs { req: Request ->
    when (val session = protocol.retrieveSession(req)) {
        is Session -> WsResponse { ws ->
            val executor = Executors.newCachedThreadPool()

            with(protocol) {
                assign(Subscription(session), ws, req)
                ws.onMessage { msg ->
                    executor.submit { receive(ws, session, req.body(msg.bodyString())) }
                }
                ws.onClose { executor.shutdown() }
                ws.send(WsMessage(SseMessage.Event("endpoint", "").toMessage()))
            }
        }

        is InvalidSession -> WsResponse { it.close(REFUSE) }
    }
}
