package org.http4k.mcp.server.sse

import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.GONE
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.protocol.ClientSessions
import org.http4k.mcp.server.protocol.SessionProvider
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class SseClientSessions(
    private val sessionProvider: SessionProvider = SessionProvider.Random(Random),
    private val keepAliveDelay: Duration = Duration.ofSeconds(2),
) : ClientSessions<Sse, Response> {

    private val sessions = ConcurrentHashMap<SessionId, Sse>()

    override fun ok() = Response(ACCEPTED)

    override fun send(sessionId: SessionId, message: McpNodeType, status: CompletionStatus) =
        when (val sink = sessions[sessionId]) {
            null -> error()
            else -> {
                sink.send(SseMessage.Event("message", McpJson.compact(message)))
                ok()
            }
        }

    override fun error() = Response(NOT_FOUND)

    override fun onClose(sessionId: SessionId, fn: () -> Unit) {
        sessions[sessionId]?.also {
            it.onClose(fn)
        }
    }

    override fun new(connectRequest: Request, transport: Sse): SessionId {
        val sessionId = sessionProvider.assign(connectRequest)
        sessions[sessionId] = transport
        return sessionId
    }

    private fun pruneDeadConnections() =
        sessions.toList().forEach { (sessionId, sink) ->
            try {
                sink.send(SseMessage.Event("ping", ""))
            } catch (e: Exception) {
                sessions.remove(sessionId)
                sink.close()
            }
        }

    fun start(executor: SimpleScheduler = SimpleSchedulerService(1)) =
        executor.scheduleWithFixedDelay(::pruneDeadConnections, keepAliveDelay, keepAliveDelay)
}
