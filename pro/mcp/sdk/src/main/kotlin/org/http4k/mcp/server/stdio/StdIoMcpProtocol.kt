package org.http4k.mcp.server.stdio

import dev.forkhandles.time.executors.SimpleScheduler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.session.McpConnection
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import org.http4k.mcp.util.readLines
import java.io.Reader
import java.io.Writer
import java.util.UUID

class StdIoMcpConnection(
    private val protocol: McpProtocol<Unit>,
    private val reader: Reader,
    private val writer: Writer
) : McpConnection<Unit, Unit> {
    override fun ok() {}

    override fun send(message: McpNodeType, sessionId: SessionId, status: CompletionStatus) = with(writer) {
        write(McpJson.compact(message) + "\n")
        flush()
    }

    override fun receive(sId: SessionId, request: Request) = protocol.receive(sId, request, this)

    override fun new(connectRequest: Request, sink: Unit) = SessionId.of(UUID(0, 0).toString())

    override fun error() = Unit

    override fun onClose(sessionId: SessionId, fn: () -> Unit) = fn()

    override fun start(executor: SimpleScheduler) {
        executor.readLines(reader) {
            try {
                protocol.receive(SessionId.of(UUID(0, 0).toString()), Request(POST, "").body(it), this)
            } catch (e: Exception) {
                e.printStackTrace(System.err)
            }
        }
    }

}

