package org.http4k.mcp.server.stdio

import dev.forkhandles.time.executors.SimpleScheduler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.capability.Completions
import org.http4k.mcp.server.capability.Logger
import org.http4k.mcp.server.capability.Prompts
import org.http4k.mcp.server.capability.Resources
import org.http4k.mcp.server.capability.Roots
import org.http4k.mcp.server.capability.Sampling
import org.http4k.mcp.server.capability.Tools
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.util.McpJson.compact
import org.http4k.mcp.util.McpNodeType
import org.http4k.mcp.util.readLines
import java.io.Reader
import java.io.Writer
import java.util.UUID
import kotlin.random.Random

class StdIoMcpProtocol(
    metaData: ServerMetaData,
    private val reader: Reader,
    private val writer: Writer,
    prompts: Prompts = Prompts(emptyList()),
    tools: Tools = Tools(emptyList()),
    resources: Resources = Resources(emptyList()),
    completions: Completions = Completions(emptyList()),
    incomingSampling: Sampling = Sampling(emptyList()),
    roots: Roots = Roots(),
    logger: Logger = Logger(),
    random: Random = Random,
) : McpProtocol<Unit>(
    metaData,
    tools,
    completions,
    resources,
    roots,
    incomingSampling,
    prompts,
    logger,
    random
) {
    override fun ok() {}

    override fun send(message: McpNodeType, sessionId: SessionId, status: CompletionStatus) = with(writer) {
        write(compact(message) + "\n")
        flush()
    }

    override fun error() = Unit

    override fun onClose(sessionId: SessionId, fn: () -> Unit) = fn()

    override fun start(executor: SimpleScheduler) =
        executor.readLines(reader) {
            try {
                receive(SessionId.of(UUID(0, 0).toString()), Request(POST, "").body(it))
            } catch (e: Exception) {
                e.printStackTrace(System.err)
            }
        }
}
