package org.http4k.mcp.server.http

import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Header
import org.http4k.lens.LAST_EVENT_ID
import org.http4k.lens.MCP_SESSION_ID
import org.http4k.lens.contentType
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.protocol.AuthedSession
import org.http4k.mcp.server.protocol.Sessions
import org.http4k.mcp.server.sessions.SessionEventStore
import org.http4k.mcp.server.sessions.SessionEventStore.Companion.NoCache
import org.http4k.mcp.server.sessions.SessionEventTracking
import org.http4k.mcp.server.sessions.SessionProvider
import org.http4k.mcp.util.McpJson.compact
import org.http4k.mcp.util.McpNodeType
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class HttpStreamingSessions(
    private val sessionProvider: SessionProvider = SessionProvider.Random(Random),
    private val sessionEventTracking: SessionEventTracking = SessionEventTracking.InMemory(),
    private val eventStore: SessionEventStore = NoCache,
    private val keepAliveDelay: Duration = Duration.ofSeconds(2)
) : Sessions<Sse, Response> {

    private val sessions = ConcurrentHashMap<SessionId, Sse>()

    override fun ok() = Response(ACCEPTED)

    override fun request(sessionId: SessionId, message: McpNodeType) =
        when (val sse = sessions[sessionId]) {
            null -> error()
            else -> {
                SseMessage.Event("message", compact(message), sessionEventTracking.next(sessionId)).also {
                    sse.send(it)
                    eventStore.write(sessionId, it)
                }
                Response(ACCEPTED)
            }
        }

    override fun respond(
        transport: Sse,
        sessionId: SessionId,
        message: McpNodeType,
        status: CompletionStatus
    ): Response {
        SseMessage.Event("message", compact(message), sessionEventTracking.next(sessionId)).also {
            eventStore.write(sessionId, it)
            transport.send(it)
        }
        return Response(OK).contentType(APPLICATION_JSON).body(compact(message))
    }

    override fun error() = Response(BAD_REQUEST)

    override fun onClose(sessionId: SessionId, fn: () -> Unit) {
        sessions[sessionId]?.also { it.onClose(fn) }
    }

    override fun validate(connectRequest: Request) =
        sessionProvider.validate(connectRequest, Header.MCP_SESSION_ID(connectRequest))

    override fun transportFor(session: AuthedSession) = sessions[session.id] ?: error("Session not found")

    override fun end(sessionId: SessionId) = ok().also {
        sessions.remove(sessionId)?.close()
        sessionEventTracking.remove(sessionId)
    }

    override fun assign(session: AuthedSession, transport: Sse, connectRequest: Request) {
        sessions[session.id] = transport
        eventStore.read(session.id, Header.LAST_EVENT_ID(connectRequest))
            .forEach(transport::send)
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
