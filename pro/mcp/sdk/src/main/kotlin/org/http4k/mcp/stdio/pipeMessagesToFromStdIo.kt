package org.http4k.mcp.stdio

import dev.forkhandles.time.executors.SimpleScheduler
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.format.jsonRpcRequest
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.readLines
import java.io.Reader
import java.util.UUID

fun SimpleScheduler.pipeMessagesToFromStdIo(protocol: StdIoMcpProtocol, reader: Reader = System.`in`.reader()): () -> Unit = {
    readLines(reader) {
        try {
            val req = Request(POST, "").body(it)
            protocol(SessionId.of(UUID(0, 0)), Body.jsonRpcRequest(McpJson).toLens()(req), req)
        } catch (e: Exception) {
            e.printStackTrace(System.err)
        }
    }
}
