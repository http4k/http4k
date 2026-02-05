package org.http4k.ai.mcp.server.http

import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.core.Request
import org.http4k.lens.Header
import org.http4k.lens.LAST_EVENT_ID
import org.http4k.lens.MCP_SESSION_ID
import org.http4k.ai.mcp.server.protocol.ClientRequestContext
import org.http4k.ai.mcp.server.protocol.ClientRequestContext.ClientCall
import org.http4k.ai.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.ai.mcp.server.protocol.Sessions
import org.http4k.ai.mcp.server.sessions.SessionEventStore
import org.http4k.ai.mcp.server.sessions.SessionEventStore.Companion.InMemory
import org.http4k.ai.mcp.server.sessions.SessionEventTracking
import org.http4k.ai.mcp.server.sessions.SessionProvider
import org.http4k.ai.mcp.util.McpJson.compact
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class HttpStreamingSessions(
    private val sessionProvider: SessionProvider = SessionProvider.Random(Random),
    private val sessionEventTracking: SessionEventTracking = SessionEventTracking.InMemory(),
    private val eventStore: SessionEventStore = InMemory(100),
    private val keepAliveDelay: Duration = Duration.ofSeconds(2)
) : Sessions<Sse> {

    private val clientConnections = ConcurrentHashMap<ClientRequestContext, Sse>()

    override fun request(context: ClientRequestContext, message: McpNodeType) {
        val sse = when (context) {
            is ClientCall -> clientConnections[context]
            is Subscription -> clientConnections[context]
        }

        when (sse) {
            null -> {}
            else -> sse.sendAndStore(message, context.session)
        }
    }

    override fun respond(transport: Sse, session: Session, message: McpNodeType): Result4k<McpNodeType, McpNodeType> {
        transport.sendAndStore(message, session)
        return Success(message)
    }

    private fun Sse.sendAndStore(message: McpNodeType, session: Session) {
        SseMessage.Event("message", compact(message), sessionEventTracking.next(session)).also {
            send(it)
            eventStore.write(session, it)
        }
    }

    override fun onClose(context: ClientRequestContext, fn: () -> Unit) {
        clientConnections[context]?.also { it.onClose(fn) }
    }

    override fun retrieveSession(connectRequest: Request) =
        sessionProvider.validate(connectRequest, Header.MCP_SESSION_ID(connectRequest))

    override fun transportFor(context: ClientRequestContext) = clientConnections[context] ?: error("Session not found")

    override fun end(context: ClientRequestContext) {
        when (context) {
            is ClientCall -> clientConnections.remove(context)
            is Subscription -> {
                clientConnections.remove(context)?.close()
                sessionEventTracking.remove(context.session)
            }
        }
    }

    override fun assign(context: ClientRequestContext, transport: Sse, connectRequest: Request) {
        clientConnections[context] = transport
        if (context is Subscription) {
            eventStore.read(context.session, Header.LAST_EVENT_ID(connectRequest)).forEach(transport::send)
        }
    }

    fun start(executor: SimpleScheduler = SimpleSchedulerService(1)) =
        executor.scheduleWithFixedDelay(::pruneDeadConnections, keepAliveDelay, keepAliveDelay)

    private fun pruneDeadConnections() =
        clientConnections
            .filterKeys { it is Subscription }
            .toList()
            .forEach { (session, sse) ->
                try {
                    sse.send(SseMessage.Event("ping", ""))
                } catch (_: Exception) {
                    clientConnections.remove(session)
                    sse.close()
                }
            }
}
