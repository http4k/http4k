package org.http4k.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.CompletionStatus.Finished
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.sessions.Session
import org.http4k.mcp.server.sessions.Session.Valid.Existing
import org.http4k.mcp.util.McpNodeType

/**
 * Responsible for managing the lifecycle of client sessions, including the assignment of
 * transport to session, and the sending of messages to the client.
 */
interface Sessions<Transport, RSP> {
    fun ok(): RSP
    fun error(): RSP
    fun respond(
        transport: Transport,
        sessionId: SessionId,
        message: McpNodeType,
        status: CompletionStatus = Finished
    ): RSP

    fun request(sessionId: SessionId, message: McpNodeType): RSP
    fun onClose(sessionId: SessionId, fn: () -> Unit)

    fun validate(connectRequest: Request): Session
    fun assign(session: Session.Valid, transport: Transport, connectRequest: Request)
    fun transportFor(session: Existing): Transport

    fun end(sessionId: SessionId): RSP
}

