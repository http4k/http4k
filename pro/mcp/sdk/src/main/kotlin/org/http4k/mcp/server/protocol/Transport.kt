package org.http4k.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.CompletionStatus.Finished
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.util.McpNodeType

interface Transport<Sink, RSP> {
    fun ok(): RSP
    fun error(): RSP
    fun send(message: McpNodeType, sessionId: SessionId, status: CompletionStatus = Finished): RSP
    fun onClose(sessionId: SessionId, fn: () -> Unit)
    fun newSession(connectRequest: Request, eventSink: Sink): SessionId
}
