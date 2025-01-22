package org.http4k.mcp.stdio

import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.format.jsonRpcRequest
import org.http4k.mcp.features.Completions
import org.http4k.mcp.features.Logger
import org.http4k.mcp.features.Prompts
import org.http4k.mcp.features.Resources
import org.http4k.mcp.features.Roots
import org.http4k.mcp.features.Sampling
import org.http4k.mcp.features.Tools
import org.http4k.mcp.protocol.AbstractMcpProtocol
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.util.McpJson
import org.http4k.sse.SseMessage
import java.io.Reader
import java.io.Writer
import java.util.UUID
import kotlin.random.Random

class StdIoMcpProtocol(
    metaData: ServerMetaData,
    prompts: Prompts = Prompts(emptyList()),
    tools: Tools = Tools(emptyList()),
    resources: Resources = Resources(emptyList()),
    completions: Completions = Completions(emptyList()),
    sampling: Sampling = Sampling(emptyList()),
    roots: Roots = Roots(),
    logger: Logger = Logger(),
    random: Random = Random,
    reader: Reader = System.`in`.reader(),
    private val writer: Writer = System.out.writer(),
    scheduler: SimpleScheduler = SimpleSchedulerService(1),
) : AbstractMcpProtocol<Unit>(metaData, tools, completions, resources, roots, sampling, prompts, logger, random) {

    init {
        scheduler.readLines(reader) {
            val req = Request(POST, "").body(it)
            this(SessionId.of(UUID(0, 0)), Body.jsonRpcRequest(McpJson).toLens()(req), req)
        }
    }

    override fun ok() {}

    override fun send(message: SseMessage, sessionId: SessionId) =
        with(writer) {
            write(message.toMessage())
            flush()
        }

    override fun error() = Unit

    override fun onClose(sessionId: SessionId, fn: () -> Unit) = fn()
}
