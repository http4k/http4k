package org.http4k.mcp.sse

import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.values.random
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.GONE
import org.http4k.mcp.capability.Completions
import org.http4k.mcp.capability.IncomingSampling
import org.http4k.mcp.capability.Logger
import org.http4k.mcp.capability.OutgoingSampling
import org.http4k.mcp.capability.Prompts
import org.http4k.mcp.capability.Resources
import org.http4k.mcp.capability.Roots
import org.http4k.mcp.capability.Tools
import org.http4k.mcp.protocol.McpProtocol
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.util.McpJson.compact
import org.http4k.mcp.util.McpNodeType
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage.Event
import org.http4k.sse.SseMessage.Ping
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class SseMcpProtocol(
    metaData: ServerMetaData,
    prompts: Prompts = Prompts(emptyList()),
    tools: Tools = Tools(emptyList()),
    resources: Resources = Resources(emptyList()),
    completions: Completions = Completions(emptyList()),
    incomingSampling: IncomingSampling = IncomingSampling(emptyList()),
    outgoingSampling: OutgoingSampling = OutgoingSampling(emptyList()),
    roots: Roots = Roots(),
    logger: Logger = Logger(),
    private val random: Random = Random,
    private val keepAliveDelay: Duration = Duration.ofSeconds(2)
) : McpProtocol<Response>(
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
    private val sessions = ConcurrentHashMap<SessionId, Sse>()

    override fun ok() = Response(ACCEPTED)

    override fun send(message: McpNodeType, sessionId: SessionId) = when (val session = sessions[sessionId]) {
        null -> Response(GONE)
        else -> {
            session.send(Event("message", compact(message)))
            Response(ACCEPTED)
        }
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

    override fun start(executor: SimpleScheduler) = executor.submit(::removeDeadConnections)

    private fun removeDeadConnections() {
        while (true) {
            Thread.sleep(keepAliveDelay.toMillis())
            sessions.toList().forEach { (sessionId, sse) ->
                try {
                    sse.send(Ping)
                } catch (e: Exception) {
                    sessions.remove(sessionId)
                    // disconnect
                    runCatching { sse.close() }.onFailure { System.err.println("ERROR CLOSING SSE") }
                }
            }
        }
    }
}
