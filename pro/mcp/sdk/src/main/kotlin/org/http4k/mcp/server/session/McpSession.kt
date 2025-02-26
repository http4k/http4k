package org.http4k.mcp.server.session

interface McpSession<Transport> {
    fun ping(transport: Transport)
    fun event(transport: Transport, data: String)
    fun onClose(transport: Transport, fn: () -> Unit)
    fun close(transport: Transport)

    companion object
}
