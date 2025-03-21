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
import org.http4k.mcp.server.protocol.ClientSessions
import org.http4k.mcp.server.protocol.SessionProvider
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import kotlin.random.Random

class JsonRpcClientSessions(private val sessionProvider: SessionProvider = SessionProvider.Random(Random)) :
    ClientSessions<Unit, Response> {

    override fun ok() = Response(ACCEPTED)

    override fun send(sessionId: SessionId, message: McpNodeType, status: CompletionStatus) =
        Response(OK).contentType(APPLICATION_JSON).body(McpJson.compact(message))

    override fun error() = Response(NOT_FOUND)

    override fun onClose(sessionId: SessionId, fn: () -> Unit) {
    }

    override fun new(connectRequest: Request, transport: Unit) = sessionProvider.assign(connectRequest)
}
