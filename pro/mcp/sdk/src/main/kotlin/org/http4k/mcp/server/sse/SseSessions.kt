package org.http4k.mcp.server.sse

import dev.forkhandles.result4k.Success
import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.server.protocol.ClientRequestContext
import org.http4k.mcp.server.protocol.ClientRequestContext.Stream
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

    private val sessions = ConcurrentHashMap<Session, Sse>()

    override fun respond(
        transport: Sse,
        session: Session,
        message: McpNodeType,
        status: CompletionStatus
    ): Success<McpNodeType> {
        transport.send(SseMessage.Event("message", compact(message), sessionEventTracking.next(session)))
        return Success(message)
    }

    override fun request(context: ClientRequestContext, message: McpNodeType) {
        when (val sse = sessions[context.session]) {
            null -> {}
            else -> sse.send(SseMessage.Event("message", compact(message), sessionEventTracking.next(context.session)))
        }
    }

    override fun onClose(session: Session, fn: () -> Unit) {
        sessions[session]?.also {
            it.onClose(fn)
        }
    }

    override fun end(context: ClientRequestContext) {
        if (context is Stream) {
            sessions.remove(context.session)?.close()
            sessionEventTracking.remove(context.session)
        }
    }

    override fun retrieveSession(connectRequest: Request) =
        sessionProvider.validate(connectRequest, sessionId(connectRequest))

    override fun transportFor(session: Session) = sessions[session] ?: error("No session")

    override fun assign(context: ClientRequestContext, transport: Sse, connectRequest: Request) {
        if (context is Stream) {
            sessions[context.session] = transport
        }
    }

    fun start(executor: SimpleScheduler = SimpleSchedulerService(1)) =
        executor.scheduleWithFixedDelay(::pruneDeadConnections, keepAliveDelay, keepAliveDelay)

    private fun pruneDeadConnections() =
        sessions.toList().forEach { (session, sse) ->
            try {
                sse.send(SseMessage.Event("ping", ""))
            } catch (e: Exception) {
                sessions.remove(session)
                sse.close()
            }
        }
}

