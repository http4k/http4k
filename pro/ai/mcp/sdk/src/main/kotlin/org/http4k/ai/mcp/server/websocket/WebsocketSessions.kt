package org.http4k.ai.mcp.server.websocket

import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService
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
import org.http4k.core.Request
import org.http4k.lens.Header
import org.http4k.lens.MCP_SESSION_ID
import org.http4k.sse.SseMessage.Event
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random


class WebsocketSessions(
    private val sessionProvider: SessionProvider = SessionProvider.Random(Random),
    private val sessionEventTracking: SessionEventTracking = SessionEventTracking.InMemory(),
    private val eventStore: SessionEventStore = InMemory(100),
    private val keepAliveDelay: Duration = Duration.ofSeconds(2),
) : Sessions<Websocket> {

    private val sessions = ConcurrentHashMap<Session, Websocket>()

    override fun respond(
        transport: Websocket,
        session: Session,
        message: McpNodeType
    ): Result4k<McpNodeType, McpNodeType> {
        transport.sendAndStore(message, session)
        return Success(message)
    }

    override fun request(context: ClientRequestContext, message: McpNodeType) =
        when (val ws = sessions[context.session]) {
            null -> Unit
            else -> ws.sendAndStore(message, context.session)
        }

    private fun Websocket.sendAndStore(message: McpNodeType, session: Session) {
        Event("message", compact(message), sessionEventTracking.next(session)).also {
            send(WsMessage(it.toMessage()))
            eventStore.write(session, it)
        }
    }

    override fun onClose(context: ClientRequestContext, fn: () -> Unit) {
        sessions[context.session]?.also { it.onClose { fn() } }
    }

    override fun retrieveSession(connectRequest: Request) =
        sessionProvider.validate(connectRequest, Header.MCP_SESSION_ID(connectRequest))

    override fun transportFor(context: ClientRequestContext) =
        sessions[context.session] ?: error("Session not found")

    override fun assign(context: ClientRequestContext, transport: Websocket, connectRequest: Request) {
        if (context is Subscription) sessions[context.session] = transport
    }

    override fun end(context: ClientRequestContext) {
        if (context is Subscription) sessions.remove(context.session)?.close()
    }

    private fun pruneDeadConnections() =
        sessions.toList().forEach { (session, sink) ->
            try {
                sink.send(WsMessage(Event("ping", "").toMessage()))
            } catch (e: Exception) {
                sessions.remove(session)
                sink.close()
            }
        }

    fun start(executor: SimpleScheduler = SimpleSchedulerService(1)) =
        executor.scheduleWithFixedDelay(::pruneDeadConnections, keepAliveDelay, keepAliveDelay)
}
