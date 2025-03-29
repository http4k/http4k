package org.http4k.mcp.server.jsonrpc

import dev.forkhandles.result4k.Success
import org.http4k.core.Request
import org.http4k.mcp.server.protocol.ClientRequestContext
import org.http4k.mcp.server.protocol.Session
import org.http4k.mcp.server.protocol.Sessions
import org.http4k.mcp.server.sessions.SessionProvider
import org.http4k.mcp.util.McpNodeType
import kotlin.random.Random

class JsonRpcSessions(private val sessionProvider: SessionProvider = SessionProvider.Random(Random)) :
    Sessions<Unit> {

    override fun respond(transport: Unit, session: Session, message: McpNodeType) =
        Success(message)

    override fun request(context: ClientRequestContext, message: McpNodeType) = error("Unsupported")

    override fun onClose(session: Session, fn: () -> Unit) {
    }

    override fun retrieveSession(connectRequest: Request) = sessionProvider.validate(connectRequest, null)

    override fun transportFor(session: Session) {
        error("Unsupported")
    }

    override fun assign(context: ClientRequestContext, transport: Unit, connectRequest: Request) {
    }

    override fun end(context: ClientRequestContext) {

    }
}
