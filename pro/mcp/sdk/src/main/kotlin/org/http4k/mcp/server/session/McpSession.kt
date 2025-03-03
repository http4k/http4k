package org.http4k.mcp.server.session

import org.http4k.mcp.model.CompletionStatus

interface McpSession<Transport> {
    fun ping(transport: Transport)
    fun event(transport: Transport, data: String, status: CompletionStatus)
    fun onClose(transport: Transport, fn: () -> Unit)
    fun close(transport: Transport)

    companion object
}
