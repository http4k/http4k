package org.http4k.mcp.server.stdio

import org.http4k.core.Request
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.protocol.ClientSessions
import org.http4k.mcp.server.protocol.Session
import org.http4k.mcp.server.protocol.Session.Valid.New
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import java.io.Writer
import java.util.UUID

class StdIoMcpClientSessions(private val writer: Writer) : ClientSessions<Unit, Unit> {
    override fun ok() {}

    override fun request(sessionId: SessionId, message: McpNodeType) = with(writer) {
        write(McpJson.compact(message) + "\n")
        flush()
    }

    override fun error() = Unit

    override fun respond(transport: Unit, sessionId: SessionId, message: McpNodeType, status: CompletionStatus) {
    }

    override fun onClose(sessionId: SessionId, fn: () -> Unit) = fn()

    override fun validate(connectRequest: Request) =
        New(SessionId.of(UUID.randomUUID().toString()))

    override fun transportFor(session: Session.Valid.Existing) {
        error("not implemented")
    }

    override fun end(sessionId: SessionId) {}

    override fun assign(session: Session, transport: Unit) {}
}
