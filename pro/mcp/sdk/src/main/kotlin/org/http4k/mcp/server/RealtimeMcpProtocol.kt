package org.http4k.mcp.server

import dev.forkhandles.time.executors.SimpleScheduler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.GONE
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.capability.CompletionCapability
import org.http4k.mcp.server.capability.Completions
import org.http4k.mcp.server.capability.IncomingSampling
import org.http4k.mcp.server.capability.IncomingSamplingCapability
import org.http4k.mcp.server.capability.Logger
import org.http4k.mcp.server.capability.OutgoingSampling
import org.http4k.mcp.server.capability.OutgoingSamplingCapability
import org.http4k.mcp.server.capability.PromptCapability
import org.http4k.mcp.server.capability.Prompts
import org.http4k.mcp.server.capability.ResourceCapability
import org.http4k.mcp.server.capability.Resources
import org.http4k.mcp.server.capability.Roots
import org.http4k.mcp.server.capability.ServerCapability
import org.http4k.mcp.server.capability.ToolCapability
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
    random: Random = Random,
    private val sessionIdFactory: SessionIdProvider = SessionIdProvider.Random(random),
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

    /**
     * Constructor useful when only simple MCP protocol behaviours are required
     */
    constructor(serverMetaData: ServerMetaData, capabilities: Array<out ServerCapability>) :
        this(
            serverMetaData,
            Prompts(capabilities.flatMap { it }.filterIsInstance<PromptCapability>()),
            Tools(capabilities.flatMap { it }.filterIsInstance<ToolCapability>()),
            Resources(capabilities.flatMap { it }.filterIsInstance<ResourceCapability>()),
            Completions(capabilities.flatMap { it }.filterIsInstance<CompletionCapability>()),
            IncomingSampling(capabilities.flatMap { it }.filterIsInstance<IncomingSamplingCapability>()),
            OutgoingSampling(capabilities.flatMap { it }.filterIsInstance<OutgoingSamplingCapability>())
        )

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

    fun newSession(connectRequest: Request, sse: Sse): SessionId {
        val sessionId = sessionIdFactory(sse.connectRequest)
        sseSessions[sessionId] = sse
        return sessionId
    }

    fun newSession(upgradeRequest: Request, websocket: Websocket): SessionId {
        val sessionId = sessionIdFactory(upgradeRequest)
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
