package org.http4k.mcp.server

import dev.forkhandles.values.random
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.GONE
import org.http4k.mcp.features.Completions
import org.http4k.mcp.features.Logger
import org.http4k.mcp.features.Prompts
import org.http4k.mcp.features.Resources
import org.http4k.mcp.features.Roots
import org.http4k.mcp.features.Sampling
import org.http4k.mcp.features.Tools
import org.http4k.mcp.protocol.McpProtocol
import org.http4k.mcp.util.McpJson
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class SseMcpProtocol(
    metaData: ServerMetaData,
    prompts: Prompts = Prompts(emptyList()),
    tools: Tools = Tools(emptyList()),
    resources: Resources = Resources(emptyList()),
    completions: Completions = Completions(emptyList()),
    sampling: Sampling = Sampling(emptyList()),
    roots: Roots = Roots(),
    logger: Logger = Logger(),
    private val random: Random = Random,
    json: McpJson = McpJson
) : McpProtocol<Response>(metaData, tools, completions, resources, roots, sampling, prompts, logger, random, json) {

    private val sessions = ConcurrentHashMap<SessionId, Sse>()

    override fun unit(unit: Unit) = Response(ACCEPTED)

    override fun send(message: SseMessage, sessionId: SessionId) = when (val session = sessions[sessionId]) {
        null -> Response(GONE)
        else -> Response(ACCEPTED).also { session.send(message) }
    }

    override fun error() = Response(GONE)

    override fun onClose(sessionId: SessionId, fn: () -> Unit) {
        sessions[sessionId]?.onClose(fn)
    }

    fun newSession(sse: Sse): SessionId {
        val sessionId = SessionId.random(random)
        sessions[sessionId] = sse
        return sessionId
    }
}
