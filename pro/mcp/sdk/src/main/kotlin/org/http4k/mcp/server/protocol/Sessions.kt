package org.http4k.mcp.server.protocol

import dev.forkhandles.result4k.Result4k
import org.http4k.core.Request
import org.http4k.mcp.util.McpNodeType

/**
 * Responsible for managing the lifecycle of client sessions, including the assignment of
 * transport to session, and the sending of messages to the client.
 */
interface Sessions<Transport> {
    fun respond(transport: Transport, session: Session, message: McpNodeType): Result4k<McpNodeType, McpNodeType>
    fun transportFor(session: Session): Transport
    fun onClose(session: Session, fn: () -> Unit)

    fun request(context: ClientRequestContext, message: McpNodeType)

    fun retrieveSession(connectRequest: Request): SessionState

    fun assign(context: ClientRequestContext, transport: Transport, connectRequest: Request)
    fun end(context: ClientRequestContext)
}

