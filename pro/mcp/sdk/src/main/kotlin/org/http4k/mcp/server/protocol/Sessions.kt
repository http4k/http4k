package org.http4k.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.CompletionStatus.Finished
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
        session: Session,
        message: McpNodeType,
        status: CompletionStatus = Finished
    ): RSP

    fun request(session: Session, message: McpNodeType): RSP
    fun onClose(session: Session, fn: () -> Unit)

    fun retrieveSession(connectRequest: Request): SessionState
    fun assign(method: ClientRequestMethod, transport: Transport, connectRequest: Request)
    fun transportFor(session: Session): Transport

    fun end(method: ClientRequestMethod): RSP
}

