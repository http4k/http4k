package org.http4k.mcp.server

import dev.forkhandles.time.executors.SimpleScheduler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.GONE
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.capability.CompletionCapability
import org.http4k.mcp.server.capability.Completions
import org.http4k.mcp.server.capability.Logger
import org.http4k.mcp.server.capability.PromptCapability
import org.http4k.mcp.server.capability.Prompts
import org.http4k.mcp.server.capability.ResourceCapability
import org.http4k.mcp.server.capability.Resources
import org.http4k.mcp.server.capability.Roots
import org.http4k.mcp.server.capability.Sampling
import org.http4k.mcp.server.capability.ServerCapability
import org.http4k.mcp.server.capability.ToolCapability
import org.http4k.mcp.server.capability.Tools
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.session.McpSession
import org.http4k.mcp.server.session.SessionProvider
import org.http4k.mcp.util.McpJson.compact
import org.http4k.mcp.util.McpNodeType
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class RealtimeMcpProtocol<Transport>(
    private val mcpSession: McpSession<Transport>,
    metaData: ServerMetaData,
    prompts: Prompts = Prompts(emptyList()),
    tools: Tools = Tools(emptyList()),
    resources: Resources = Resources(emptyList()),
    completions: Completions = Completions(emptyList()),
    sampling: Sampling = Sampling(),
    roots: Roots = Roots(),
    logger: Logger = Logger(),
    random: Random = Random,
    private val sessionProvider: SessionProvider = SessionProvider.Random(random),
    private val keepAliveDelay: Duration = Duration.ofSeconds(2),
) : McpProtocol<Response>(
    metaData,
    tools,
    completions,
    resources,
    roots,
    sampling,
    prompts,
    logger,
    random
) {
    /**
     * Constructor useful when only simple MCP protocol behaviours are required
     */
    constructor(
        mcpSession: McpSession<Transport>,
        serverMetaData: ServerMetaData, capabilities: Array<out ServerCapability>
    ) :
        this(
            mcpSession,
            serverMetaData,
            Prompts(capabilities.flatMap { it }.filterIsInstance<PromptCapability>()),
            Tools(capabilities.flatMap { it }.filterIsInstance<ToolCapability>()),
            Resources(capabilities.flatMap { it }.filterIsInstance<ResourceCapability>()),
            Completions(capabilities.flatMap { it }.filterIsInstance<CompletionCapability>()),
        )

    private val sessions = ConcurrentHashMap<SessionId, Transport>()

    override fun ok() = Response(ACCEPTED)

    override fun send(message: McpNodeType, sessionId: SessionId, status: CompletionStatus) = when (val sink = sessions[sessionId]) {
        null -> Response(GONE)
        else -> {
            mcpSession.event(sink, compact(message), status)
            Response(ACCEPTED)
        }
    }

    override fun error() = Response(GONE)

    override fun onClose(sessionId: SessionId, fn: () -> Unit) {
        sessions[sessionId]?.also { mcpSession.onClose(it, fn) }
    }

    fun newSession(connectRequest: Request, eventSink: Transport): SessionId {
        val sessionId = sessionProvider.assign(connectRequest)
        sessions[sessionId] = eventSink
        return sessionId
    }

    private fun pruneDeadConnections() =
        sessions.toList().forEach { (sessionId, sink) ->
            try {
                mcpSession.ping(sink)
            } catch (e: Exception) {
                sessions.remove(sessionId)
                mcpSession.close(sink)
            }
        }

    override fun start(executor: SimpleScheduler) =
        executor.scheduleWithFixedDelay(::pruneDeadConnections, keepAliveDelay, keepAliveDelay)
}
