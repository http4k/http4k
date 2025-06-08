package org.http4k.ai.mcp.server.stdio

import dev.forkhandles.result4k.Success
import org.http4k.core.Request
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.server.protocol.ClientRequestContext
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.ai.mcp.server.protocol.Sessions
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType
import java.io.Writer
import java.util.UUID

class StdIoMcpSessions(private val writer: Writer) : Sessions<Unit> {

    override fun request(context: ClientRequestContext, message: McpNodeType) = with(writer) {
        write(McpJson.compact(message) + "\n")
        flush()
    }

    override fun respond(transport: Unit, session: Session, message: McpNodeType) =
        Success(message)

    override fun onClose(context: ClientRequestContext, fn: () -> Unit) = fn()

    override fun retrieveSession(connectRequest: Request) =
        Session(SessionId.of(UUID.randomUUID().toString()))

    override fun transportFor(context: ClientRequestContext) {
        error("not implemented")
    }

    override fun end(context: ClientRequestContext) {}

    override fun assign(context: ClientRequestContext, transport: Unit, connectRequest: Request) {}
}
