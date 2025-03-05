package org.http4k.mcp.server.http

import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.CompletionStatus.Finished
import org.http4k.mcp.server.session.McpSession
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage

/**
 * HTTP session connection.
 */
fun McpSession.Companion.Http() = object : McpSession<Sse> {
    override fun ping(sink: Sse) {
    }

    override fun event(sink: Sse, data: String, status: CompletionStatus) {
        sink.send(SseMessage.Event("message", data))
        if (status == Finished) close(sink)
    }

    override fun onClose(sink: Sse, fn: () -> Unit) {
        sink.onClose(fn)
    }

    override fun close(sink: Sse) = sink.close()
}
