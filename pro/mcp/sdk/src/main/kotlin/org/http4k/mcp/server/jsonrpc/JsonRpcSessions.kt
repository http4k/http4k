package org.http4k.mcp.server.jsonrpc

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.contentType
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.protocol.Sessions
import org.http4k.mcp.server.sessions.Session
import org.http4k.mcp.server.sessions.SessionProvider
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import kotlin.random.Random

class JsonRpcSessions(private val sessionProvider: SessionProvider = SessionProvider.Random(Random)) :
    Sessions<Unit, Response> {

    override fun ok() = Response(ACCEPTED)

    override fun respond(transport: Unit, sessionId: SessionId, message: McpNodeType, status: CompletionStatus) =
        Response(OK).contentType(APPLICATION_JSON).body(McpJson.compact(message))

    override fun request(sessionId: SessionId, message: McpNodeType) =
        Response(OK).contentType(APPLICATION_JSON).body(McpJson.compact(message))

    override fun error() = Response(NOT_FOUND)

    override fun onClose(sessionId: SessionId, fn: () -> Unit) {
    }

    override fun validate(connectRequest: Request) = sessionProvider.validate(connectRequest, null)

    override fun transportFor(session: Session.Valid) {
        error("not implemented")
    }

    override fun assign(session: Session.Valid, transport: Unit, connectRequest: Request) {
    }

    override fun end(sessionId: SessionId) = ok()
}
