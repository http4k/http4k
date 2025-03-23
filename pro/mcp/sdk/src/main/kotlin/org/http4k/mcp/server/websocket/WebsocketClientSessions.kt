package org.http4k.mcp.server.websocket

import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.core.Request
import org.http4k.lens.Header
import org.http4k.lens.MCP_SESSION_ID
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.protocol.ClientSessions
import org.http4k.mcp.server.protocol.Session
import org.http4k.mcp.server.protocol.Session.Invalid
import org.http4k.mcp.server.protocol.Session.Valid
import org.http4k.mcp.server.protocol.SessionProvider
import org.http4k.mcp.util.McpJson.compact
import org.http4k.mcp.util.McpNodeType
import org.http4k.sse.SseMessage.Event
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class WebsocketClientSessions(
    private val sessionProvider: SessionProvider = SessionProvider.Random(Random),
    private val keepAliveDelay: Duration = Duration.ofSeconds(2),
) : ClientSessions<Websocket, Unit> {

    private val sessions = ConcurrentHashMap<SessionId, Websocket>()

    override fun ok() = Unit

    override fun respond(transport: Websocket, sessionId: SessionId, message: McpNodeType, status: CompletionStatus) {
        transport.send(WsMessage(Event("message", compact(message)).toMessage()))
    }

    override fun request(sessionId: SessionId, message: McpNodeType) =
        when (val ws = sessions[sessionId]) {
            null -> Unit
            else -> ws.send(WsMessage(Event("message", compact(message)).toMessage()))
        }

    override fun error() = Unit

    override fun onClose(sessionId: SessionId, fn: () -> Unit) {
        sessions[sessionId]?.also { it.onClose { fn() } }
    }

    override fun validate(connectRequest: Request) =
        sessionProvider.validate(connectRequest, Header.MCP_SESSION_ID(connectRequest))

    override fun transportFor(session: Valid.Existing) =
        sessions[session.sessionId] ?: error("Session not found")

    override fun assign(session: Session, transport: Websocket) {
        when (session) {
            is Valid -> sessions[session.sessionId] = transport
            is Invalid -> {}
        }
    }

    override fun end(sessionId: SessionId) = ok().also { sessions.remove(sessionId)?.close() }

    private fun pruneDeadConnections() =
        sessions.toList().forEach { (sessionId, sink) ->
            try {
                sink.send(WsMessage(Event("ping", "").toMessage()))
            } catch (e: Exception) {
                sessions.remove(sessionId)
                sink.close()
            }
        }

    fun start(executor: SimpleScheduler = SimpleSchedulerService(1)) =
        executor.scheduleWithFixedDelay(::pruneDeadConnections, keepAliveDelay, keepAliveDelay)
}
