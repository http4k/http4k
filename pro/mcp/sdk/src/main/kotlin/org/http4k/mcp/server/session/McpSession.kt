package org.http4k.mcp.server.session

import org.http4k.sse.SseMessage

interface McpSession<Transport> {
    fun send(transport: Transport, event: SseMessage.Event)
    fun onClose(transport: Transport, fn: () -> Unit)
    fun close(transport: Transport)

    companion object
}
