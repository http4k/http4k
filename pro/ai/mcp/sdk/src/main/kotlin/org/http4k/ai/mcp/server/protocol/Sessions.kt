package org.http4k.ai.mcp.server.protocol

import dev.forkhandles.result4k.Result4k
import org.http4k.core.Request
import org.http4k.ai.mcp.util.McpNodeType

/**
 * Responsible for managing the lifecycle of client sessions, including the assignment of
 * transport to session, and the sending of messages to the client.
 */
interface Sessions<Transport> {
    fun retrieveSession(connectRequest: Request): org.http4k.ai.mcp.server.protocol.SessionState

    fun respond(transport: Transport, session: org.http4k.ai.mcp.server.protocol.Session, message: McpNodeType): Result4k<McpNodeType, McpNodeType>
    fun transportFor(context: org.http4k.ai.mcp.server.protocol.ClientRequestContext): Transport
    fun onClose(context: org.http4k.ai.mcp.server.protocol.ClientRequestContext, fn: () -> Unit)

    fun request(context: org.http4k.ai.mcp.server.protocol.ClientRequestContext, message: McpNodeType)

    fun assign(context: org.http4k.ai.mcp.server.protocol.ClientRequestContext, transport: Transport, connectRequest: Request)
    fun end(context: org.http4k.ai.mcp.server.protocol.ClientRequestContext)
}

