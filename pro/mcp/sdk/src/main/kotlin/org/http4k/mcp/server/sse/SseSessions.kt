package org.http4k.mcp.server.sse

import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.protocol.Session
import org.http4k.mcp.server.protocol.Sessions
import org.http4k.mcp.server.sessions.SessionEventTracking
import org.http4k.mcp.server.sessions.SessionProvider
import org.http4k.mcp.util.McpJson.compact
import org.http4k.mcp.util.McpNodeType
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class SseSessions(
    private val sessionProvider: SessionProvider = SessionProvider.Random(Random),
    private val sessionEventTracking: SessionEventTracking = SessionEventTracking.InMemory(),
    private val keepAliveDelay: Duration = Duration.ofSeconds(2),
) : Sessions<Sse, Response> {

    private val sessions = ConcurrentHashMap<SessionId, Sse>()

    override fun ok() = Response(ACCEPTED)

    override fun respond(
        transport: Sse,
        sessionId: SessionId,
        message: McpNodeType,
        status: CompletionStatus
    ): Response {
        transport.send(SseMessage.Event("message", compact(message), sessionEventTracking.next(sessionId)))
        return Response(ACCEPTED)
    }

    override fun request(sessionId: SessionId, message: McpNodeType) =
        when (val sse = sessions[sessionId]) {
            null -> error()
            else -> {
                sse.send(SseMessage.Event("message", compact(message), sessionEventTracking.next(sessionId)))
                ok()
            }
        }

    override fun error() = Response(NOT_FOUND)

    override fun onClose(sessionId: SessionId, fn: () -> Unit) {
        sessions[sessionId]?.also {
            it.onClose(fn)
        }
    }

    override fun end(sessionId: SessionId) = ok().also {
        sessions.remove(sessionId)?.close()
        sessionEventTracking.remove(sessionId)
    }

    override fun retrieveSession(connectRequest: Request) = sessionProvider.validate(connectRequest, sessionId(connectRequest))
    override fun transportFor(session: Session) = sessions[session.id] ?: error("No session")

    override fun assign(session: Session, transport: Sse, connectRequest: Request) {
        sessions[session.id] = transport
    }

    fun start(executor: SimpleScheduler = SimpleSchedulerService(1)) =
        executor.scheduleWithFixedDelay(::pruneDeadConnections, keepAliveDelay, keepAliveDelay)

    private fun pruneDeadConnections() =
        sessions.toList().forEach { (sessionId, sse) ->
            try {
                sse.send(SseMessage.Event("ping", ""))
            } catch (e: Exception) {
                sessions.remove(sessionId)
                sse.close()
            }
        }
}

