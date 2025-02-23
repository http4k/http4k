package org.http4k.mcp.server

import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.values.random
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.GONE
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.capability.Completions
import org.http4k.mcp.server.capability.IncomingSampling
import org.http4k.mcp.server.capability.Logger
import org.http4k.mcp.server.capability.OutgoingSampling
import org.http4k.mcp.server.capability.Prompts
import org.http4k.mcp.server.capability.Resources
import org.http4k.mcp.server.capability.Roots
import org.http4k.mcp.server.capability.Tools
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.util.McpJson.compact
import org.http4k.mcp.util.McpNodeType
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage.Event
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class RealtimeMcpProtocol(
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
    private val sseSessions = ConcurrentHashMap<SessionId, Sse>()
    private val wsSessions = ConcurrentHashMap<SessionId, Websocket>()

    override fun ok() = Response(ACCEPTED)

    override fun send(message: McpNodeType, sessionId: SessionId) = when (val sse = sseSessions[sessionId]) {
        null -> {
            when (val wsSession = wsSessions[sessionId]) {
                null -> Response(GONE)
                else -> {
                    wsSession.send(WsMessage(Event("message", compact(message)).toMessage()))
                    Response(ACCEPTED)
                }
            }
        }

        else -> {
            sse.send(Event("message", compact(message)))
            Response(ACCEPTED)
        }
    }

    override fun error() = Response(GONE)

    override fun onClose(sessionId: SessionId, fn: () -> Unit) {
        sseSessions[sessionId]?.onClose(fn)
    }

    fun newSession(sse: Sse): SessionId {
        val sessionId = SessionId.random(random)
        sseSessions[sessionId] = sse
        return sessionId
    }

    fun newSession(websocket: Websocket): SessionId {
        val sessionId = SessionId.random(random)
        wsSessions[sessionId] = websocket
        return sessionId
    }

    private fun pruneDeadConnections() =
        sseSessions.toList().forEach { (sessionId, sse) ->
            try {
                sse.send(Event("ping", ""))
            } catch (e: Exception) {
                sseSessions.remove(sessionId)
                sse.close()
            }
        }

    override fun start(executor: SimpleScheduler) =
        executor.scheduleWithFixedDelay(::pruneDeadConnections, keepAliveDelay, keepAliveDelay)
}
