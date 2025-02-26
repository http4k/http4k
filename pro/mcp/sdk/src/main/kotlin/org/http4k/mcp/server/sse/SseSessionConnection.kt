package org.http4k.mcp.server.sse

import org.http4k.mcp.server.session.McpSession
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage

/**
 * SSE session connection.
 */
fun McpSession.Companion.Sse() = object : McpSession<Sse> {
    override fun send(transport: Sse, event: SseMessage.Event) {
        transport.send(event)
    }

    override fun onClose(transport: Sse, fn: () -> Unit) {
        transport.onClose(fn)
    }

    override fun close(transport: Sse) = transport.close()
}
