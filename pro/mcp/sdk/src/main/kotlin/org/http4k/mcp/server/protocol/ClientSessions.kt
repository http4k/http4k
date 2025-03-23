package org.http4k.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.CompletionStatus.Finished
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.protocol.Session.Valid.Existing
import org.http4k.mcp.util.McpNodeType

/**
 * A ClientSessions is responsible for managing the lifecycle of client sessions, including the assignment of
 * transport to session, and the sending of messages to the client.
 */
interface ClientSessions<Transport, RSP> {
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
    fun assign(session: Session, transport: Transport)
    fun transportFor(session: Existing): Transport

    fun end(sessionId: SessionId): RSP
}

