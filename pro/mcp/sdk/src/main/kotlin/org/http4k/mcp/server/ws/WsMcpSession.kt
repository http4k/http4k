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
    override fun ping(sink: Websocket) {
        sink.send(WsMessage(Event("ping", "").toMessage()))
    }

    override fun event(sink: Websocket, data: String, status: CompletionStatus) {
        sink.send(WsMessage(Event("message", data).toMessage()))
    }

    override fun onClose(sink: Websocket, fn: () -> Unit) {
        sink.onClose { fn() }
    }

    override fun close(sink: Websocket) = sink.close()
}
