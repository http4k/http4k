package org.http4k.mcp.server.session

import org.http4k.mcp.model.CompletionStatus

interface McpSession<Sink> {
    fun ping(sink: Sink)
    fun event(sink: Sink, data: String, status: CompletionStatus)
    fun onClose(sink: Sink, fn: () -> Unit)
    fun close(sink: Sink)

    companion object
}
