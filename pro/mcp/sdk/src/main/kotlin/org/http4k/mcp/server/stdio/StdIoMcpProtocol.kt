package org.http4k.mcp.server.stdio

import org.http4k.core.Request
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.protocol.McpTransport
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import java.io.Writer
import java.util.UUID

class StdIoMcpTransport(private val writer: Writer) : McpTransport<Unit, Unit> {

    override fun ok() {}

    override fun send(message: McpNodeType, sessionId: SessionId, status: CompletionStatus) = with(writer) {
        write(McpJson.compact(message) + "\n")
        flush()
    }

    override fun verify(sessionId: SessionId, request: Request) = true

    override fun newSession(connectRequest: Request, sink: Unit) = SessionId.of(UUID(0, 0).toString())

    override fun error() = Unit

    override fun onClose(sessionId: SessionId, fn: () -> Unit) = fn()
}

