package org.http4k.mcp.server.jsonrpc

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.GONE
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.contentType
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.protocol.Transport
import org.http4k.mcp.server.protocol.SessionProvider
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import kotlin.random.Random

class JsonRpcTransport(private val sessionProvider: SessionProvider = SessionProvider.Random(Random)) :
    Transport<Unit, Response> {

    override fun ok() = Response(ACCEPTED)

    override fun send(message: McpNodeType, sessionId: SessionId, status: CompletionStatus) =
        Response(OK).contentType(APPLICATION_JSON).body(McpJson.compact(message))

    override fun error() = Response(GONE)

    override fun onClose(sessionId: SessionId, fn: () -> Unit) {
    }

    override fun newSession(connectRequest: Request, eventSink: Unit) = sessionProvider.assign(connectRequest)
}
