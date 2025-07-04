package org.http4k.ai.mcp.server.jsonrpc

import dev.forkhandles.result4k.Success
import org.http4k.core.Request
import org.http4k.ai.mcp.server.protocol.ClientRequestContext
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.ai.mcp.server.protocol.Sessions
import org.http4k.ai.mcp.server.sessions.SessionProvider
import org.http4k.ai.mcp.util.McpNodeType
import kotlin.random.Random

class JsonRpcSessions(private val sessionProvider: SessionProvider = SessionProvider.Random(Random)) :
    org.http4k.ai.mcp.server.protocol.Sessions<Unit> {

    override fun respond(transport: Unit, session: Session, message: McpNodeType) =
        Success(message)

    override fun request(context: ClientRequestContext, message: McpNodeType) = error("Unsupported")

    override fun onClose(context: ClientRequestContext, fn: () -> Unit) {
    }

    override fun retrieveSession(connectRequest: Request) = sessionProvider.validate(connectRequest, null)

    override fun transportFor(context: ClientRequestContext) {
        error("Unsupported")
    }

    override fun assign(context: ClientRequestContext, transport: Unit, connectRequest: Request) {
    }

    override fun end(context: ClientRequestContext) {

    }
}
