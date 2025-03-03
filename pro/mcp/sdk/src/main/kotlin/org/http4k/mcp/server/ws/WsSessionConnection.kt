package org.http4k.mcp.server.ws

import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.server.session.McpSession
import org.http4k.sse.SseMessage.Event
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage

/**
 * Websocket session connection.
 */
fun McpSession.Companion.Websocket() = object : McpSession<Websocket> {
    override fun ping(transport: Websocket) {
        transport.send(WsMessage(Event("ping", "").toMessage()))
    }

    override fun event(transport: Websocket, data: String, status: CompletionStatus) {
        transport.send(WsMessage(Event("message", data).toMessage()))
    }

    override fun onClose(transport: Websocket, fn: () -> Unit) {
        transport.onClose { fn() }
    }

    override fun close(transport: Websocket) = transport.close()
}
