package org.http4k.ai.mcp.server.sse

import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.core.Request
import org.http4k.ai.mcp.server.protocol.ClientRequestContext
import org.http4k.ai.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.ai.mcp.server.protocol.Sessions
import org.http4k.ai.mcp.server.sessions.SessionEventStore
import org.http4k.ai.mcp.server.sessions.SessionEventStore.Companion.InMemory
import org.http4k.ai.mcp.server.sessions.SessionEventTracking
import org.http4k.ai.mcp.server.sessions.SessionProvider
import org.http4k.ai.mcp.util.McpJson.compact
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.routing.sse
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class SseSessions(
    private val sessionProvider: SessionProvider = SessionProvider.Random(Random),
    private val sessionEventTracking: SessionEventTracking = SessionEventTracking.InMemory(),
    private val eventStore: SessionEventStore = InMemory(100),
    private val keepAliveDelay: Duration = Duration.ofSeconds(2),
) : Sessions<Sse> {

    private val sessions = ConcurrentHashMap<Session, Sse>()

    override fun respond(
        transport: Sse,
        session: Session,
        message: McpNodeType
    ): Result4k<McpNodeType, McpNodeType> {
        transport.sendAndStore(message, session)
        return Success(message)
    }

    override fun request(context: ClientRequestContext, message: McpNodeType) {
        when (val sse = sessions[context.session]) {
            null -> {}
            else -> sse.sendAndStore(message, context.session)
        }
    }

    private fun Sse.sendAndStore(message: McpNodeType, session: Session) {
        SseMessage.Event("message", compact(message), sessionEventTracking.next(session)).also {
            send(it)
            eventStore.write(session, it)
        }
    }

    override fun onClose(context: ClientRequestContext, fn: () -> Unit) {
        sessions[context.session]?.also { it.onClose(fn) }
    }

    override fun end(context: ClientRequestContext) {
        if (context is Subscription) {
            sessions.remove(context.session)?.close()
            sessionEventTracking.remove(context.session)
        }
    }

    override fun retrieveSession(connectRequest: Request) =
        sessionProvider.validate(connectRequest, sessionId(connectRequest))

    override fun transportFor(context: ClientRequestContext) = sessions[context.session] ?: error("No session")

    override fun assign(context: ClientRequestContext, transport: Sse, connectRequest: Request) {
        if (context is Subscription) {
            sessions[context.session] = transport
        }
    }

    fun start(executor: SimpleScheduler = SimpleSchedulerService(1)) =
        executor.scheduleWithFixedDelay(::pruneDeadConnections, keepAliveDelay, keepAliveDelay)

    private fun pruneDeadConnections() =
        sessions.toList().forEach { (session, sse) ->
            try {
                sse.send(SseMessage.Event("ping", ""))
            } catch (_: Exception) {
                sessions.remove(session)
                sse.close()
            }
        }
}

