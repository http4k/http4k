/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.websocket

import org.http4k.ai.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.ai.mcp.server.protocol.InvalidSessionState
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.protocol.ValidSessionState
import org.http4k.ai.mcp.server.sse.sessionId
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.MCP_SESSION_ID
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
    when (val sessionState = protocol.retrieveSession(req)) {
        is ValidSessionState -> WsResponse { ws ->
            val executor = Executors.newVirtualThreadPerTaskExecutor()

            val context = Subscription(sessionState.session)

            with(protocol) {
                var firstCall = true

                ws.onMessage { msg ->
                    executor.submit {
                        receive(ws, sessionToUse(firstCall, protocol, req, sessionState), req.body(msg.bodyString()))
                        firstCall = false
                    }
                }
                ws.onClose {
                    protocol.end(context)
                    executor.shutdown()
                }
                ws.send(
                    WsMessage(
                        SseMessage.Event(
                            "endpoint",
                            Request(GET, "/message").with(sessionId of sessionState.session.id).uri.toString()
                        )
                            .toMessage()
                    )
                )
            }
        }

        InvalidSessionState -> WsResponse { it.close(REFUSE) }
    }
}

private fun sessionToUse(
    isFirst: Boolean,
    protocol: McpProtocol<Websocket>,
    req: Request,
    sessionState: ValidSessionState
) = when {
    isFirst -> sessionState
    else -> protocol.retrieveSession(req.with(Header.MCP_SESSION_ID of sessionState.session.id)) as? ValidSessionState ?: sessionState
}
