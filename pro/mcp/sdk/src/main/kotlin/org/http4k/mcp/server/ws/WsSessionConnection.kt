package org.http4k.mcp.server.ws

import org.http4k.mcp.server.session.McpSession
import org.http4k.sse.SseMessage.Event
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage

/**
 * Websocket session connection.
 */
fun McpSession.Companion.Websocket() = object : McpSession<Websocket> {
    override fun send(transport: Websocket, event: Event) {
        transport.send(WsMessage(event.toMessage()))
    }

    override fun onClose(transport: Websocket, fn: () -> Unit) {
        transport.onClose { fn() }
    }

    override fun close(transport: Websocket) = transport.close()
}
