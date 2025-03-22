package org.http4k.mcp.server.sse

import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.protocol.ClientSessions
import org.http4k.mcp.server.protocol.Session
import org.http4k.mcp.server.protocol.Session.Invalid
import org.http4k.mcp.server.protocol.Session.Valid
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

    override fun respond(
        transport: Sse,
        sessionId: SessionId,
        message: McpNodeType,
        status: CompletionStatus
    ): Response {
        val data = McpJson.compact(message)
        transport.send(SseMessage.Event("message", data))
        return Response(ACCEPTED)
    }

    override fun request(sessionId: SessionId, message: McpNodeType, status: CompletionStatus) =
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

    override fun end(session: Session) = when (session) {
        Invalid -> error()
        is Valid -> ok().also { sessions.remove(session.sessionId)?.close() }
    }

    override fun validate(connectRequest: Request) = sessionProvider.validate(connectRequest, sessionId(connectRequest))
    override fun transportFor(session: Valid.Existing) = sessions[session.sessionId] ?: error("No session")

    override fun assign(session: Session, transport: Sse) {
        when (session) {
            is Valid -> sessions[session.sessionId] = transport
            is Invalid -> {}
        }
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
