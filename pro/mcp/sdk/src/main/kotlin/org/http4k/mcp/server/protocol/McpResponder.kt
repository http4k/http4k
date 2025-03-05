package org.http4k.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.CompletionStatus.Finished
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.util.McpNodeType

interface McpResponder<RSP : Any> {
    fun ok(): RSP
    fun error(): RSP
    fun receive(sId: SessionId, request: Request): RSP
    fun send(message: McpNodeType, sessionId: SessionId, status: CompletionStatus = Finished): RSP
}
