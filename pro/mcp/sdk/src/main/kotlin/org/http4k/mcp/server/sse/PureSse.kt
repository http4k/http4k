package org.http4k.mcp.server.sse

import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.CompletionStatus.Finished
import org.http4k.mcp.server.session.McpSession
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage

/**
 * SSE session connection.
 */
fun McpSession.Companion.PureSse() = object : McpSession<Sse> {
    override fun ping(transport: Sse) {
        transport.send(SseMessage.Event("ping", ""))
    }

    override fun event(transport: Sse, data: String, status: CompletionStatus) {
        transport.send(SseMessage.Event("message", data))
        if (status == Finished) close(transport)
    }

    override fun onClose(transport: Sse, fn: () -> Unit) {
        transport.onClose(fn)
    }

    override fun close(transport: Sse) = transport.close()
}
