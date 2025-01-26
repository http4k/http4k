package org.http4k.mcp.stdio

import org.http4k.mcp.features.Completions
import org.http4k.mcp.features.IncomingSampling
import org.http4k.mcp.features.Logger
import org.http4k.mcp.features.OutgoingSampling
import org.http4k.mcp.features.Prompts
import org.http4k.mcp.features.Resources
import org.http4k.mcp.features.Roots
import org.http4k.mcp.features.Tools
import org.http4k.mcp.protocol.McpProtocol
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.util.McpJson.compact
import org.http4k.mcp.util.McpNodeType
import java.io.Writer
import kotlin.random.Random

class StdIoMcpProtocol(
    metaData: ServerMetaData,
    private val writer: Writer,
    prompts: Prompts = Prompts(emptyList()),
    tools: Tools = Tools(emptyList()),
    resources: Resources = Resources(emptyList()),
    completions: Completions = Completions(emptyList()),
    incomingSampling: IncomingSampling = IncomingSampling(emptyList()),
    outgoingSampling: OutgoingSampling = OutgoingSampling(emptyList()),
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
    outgoingSampling,
    prompts,
    logger,
    random
) {

    override fun ok() {}

    override fun send(message: McpNodeType, sessionId: SessionId) = with(writer) {
        write(compact(message) + "\n")
        flush()
    }

    override fun error() = Unit

    override fun onClose(sessionId: SessionId, fn: () -> Unit) = fn()
}
