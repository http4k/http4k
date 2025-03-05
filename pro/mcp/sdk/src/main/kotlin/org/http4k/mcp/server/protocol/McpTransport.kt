package org.http4k.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.CompletionStatus.Finished
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.util.McpNodeType

interface McpTransport<RSP : Any, Sink> {
    fun ok(): RSP
    fun error(): RSP
    fun send(message: McpNodeType, sessionId: SessionId, status: CompletionStatus = Finished): RSP
    fun newSession(connectRequest: Request, sink: Sink): SessionId
    fun onClose(sessionId: SessionId, fn: () -> Unit)
}
