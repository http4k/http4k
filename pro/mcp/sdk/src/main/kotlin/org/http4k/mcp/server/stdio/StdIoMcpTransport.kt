package org.http4k.mcp.server.stdio

import org.http4k.core.Request
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.protocol.Transport
import org.http4k.mcp.util.McpNodeType
import java.io.Reader
import java.io.Writer
import java.util.UUID

// TODO fix the reading of the lines
class StdIoMcpTransport(
    private val reader: Reader,
    private val writer: Writer,
) : Transport<Unit, Unit> {
    override fun ok() {}

    override fun send(message: McpNodeType, sessionId: SessionId, status: CompletionStatus) = with(writer) {
        write(org.http4k.mcp.util.McpJson.compact(message) + "\n")
        flush()
    }

    override fun error() = Unit

    override fun onClose(sessionId: SessionId, fn: () -> Unit) = fn()

    override fun newSession(connectRequest: Request, eventSink: Unit) = SessionId.of(UUID.randomUUID().toString())
//
//    fun start(executor: SimpleScheduler) =
//        executor.readLines(reader) {
//            try {
//                receive(SessionId.of(UUID(0, 0).toString()), Request(POST, "").body(it))
//            } catch (e: Exception) {
//                e.printStackTrace(System.err)
//            }
//        }

}
