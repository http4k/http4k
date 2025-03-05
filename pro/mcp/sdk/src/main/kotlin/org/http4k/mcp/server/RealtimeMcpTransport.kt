package org.http4k.mcp.server

import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.GONE
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.protocol.McpTransport
import org.http4k.mcp.server.session.McpSession
import org.http4k.mcp.server.session.SessionProvider
import org.http4k.mcp.util.McpJson.compact
import org.http4k.mcp.util.McpNodeType
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class RealtimeMcpTransport<Sink>(
    private val mcpSession: McpSession<Sink>,
    private val sessionProvider: SessionProvider = SessionProvider.Random(Random),
    private val keepAliveDelay: Duration = Duration.ofSeconds(2),
) : McpTransport<Response, Sink> {
    private val sessions = ConcurrentHashMap<SessionId, Sink>()

    override fun ok() = Response(ACCEPTED)

    override fun send(message: McpNodeType, sessionId: SessionId, status: CompletionStatus) =
        when (val sink = sessions[sessionId]) {
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

    override fun newSession(connectRequest: Request, sink: Sink): SessionId {
        val sessionId = sessionProvider.assign(connectRequest)
        sessions[sessionId] = sink
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

    fun start(executor: SimpleScheduler = SimpleSchedulerService(1)) =
        executor.scheduleWithFixedDelay(::pruneDeadConnections, keepAliveDelay, keepAliveDelay)
}
