package org.http4k.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.CompletionStatus.Finished
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.util.McpNodeType

interface ClientSessions<Transport, RSP> {
    fun ok(): RSP
    fun error(): RSP
    fun send(sessionId: SessionId, message: McpNodeType, status: CompletionStatus = Finished): RSP
    fun onClose(sessionId: SessionId, fn: () -> Unit)
    fun new(connectRequest: Request, transport: Transport): SessionId
}
