package org.http4k.mcp.server.stdio

import dev.forkhandles.result4k.Success
import org.http4k.core.Request
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.protocol.ClientRequestContext
import org.http4k.mcp.server.protocol.Session
import org.http4k.mcp.server.protocol.Sessions
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import java.io.Writer
import java.util.UUID

class StdIoMcpSessions(private val writer: Writer) : Sessions<Unit, Unit> {

    override fun request(context: ClientRequestContext, message: McpNodeType) = with(writer) {
        write(McpJson.compact(message) + "\n")
        flush()
    }

    override fun respond(transport: Unit, session: Session, message: McpNodeType, status: CompletionStatus) =
        Success(message)

    override fun onClose(session: Session, fn: () -> Unit) = fn()

    override fun retrieveSession(connectRequest: Request) =
        Session(SessionId.of(UUID.randomUUID().toString()))

    override fun transportFor(session: Session) {
        error("not implemented")
    }

    override fun end(context: ClientRequestContext) {}

    override fun assign(context: ClientRequestContext, transport: Unit, connectRequest: Request) {}
}
