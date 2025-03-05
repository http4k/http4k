package org.http4k.mcp.server.sse

import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.server.session.McpSession
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage

/**
 * SSE session connection.
 */
fun McpSession.Companion.SseSession() = object : McpSession<Sse> {
    override fun ping(sink: Sse) {
        sink.send(SseMessage.Event("ping", ""))
    }

    override fun event(sink: Sse, data: String, status: CompletionStatus) {
        sink.send(SseMessage.Event("message", data))
    }

    override fun onClose(sink: Sse, fn: () -> Unit) {
        sink.onClose(fn)
    }

    override fun close(sink: Sse) = sink.close()
}
