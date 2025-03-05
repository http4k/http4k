package org.http4k.mcp.server.ws

import dev.forkhandles.time.executors.SimpleScheduler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.GONE
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.session.McpSession
import org.http4k.mcp.server.session.SessionProvider
import org.http4k.mcp.util.McpJson.compact
import org.http4k.mcp.util.McpNodeType
import org.http4k.sse.SseMessage.Event
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class WsMcpSession(
    private val protocol: McpProtocol<Response>,
    private val sessionProvider: SessionProvider = SessionProvider.Random(Random),
    private val keepAliveDelay: Duration = Duration.ofSeconds(2),
) : McpSession<Response, Websocket> {
    private val sessions = ConcurrentHashMap<SessionId, Websocket>()

    override fun ok() = Response(ACCEPTED)

    override fun send(message: McpNodeType, sessionId: SessionId, status: CompletionStatus) =
        when (val sink = sessions[sessionId]) {
            null -> Response(GONE)
            else -> {
                sink.send(WsMessage(Event("message", compact(message)).toMessage()))
                Response(ACCEPTED)
            }
        }

    override fun receive(sId: SessionId, request: Request) = when {
        sessionProvider.verify(sId, request) -> protocol.receive(sId, request, this)
        else -> error()
    }

    override fun error() = Response(GONE)

    override fun onClose(sessionId: SessionId, fn: () -> Unit) {
        sessions[sessionId]?.also {
            it.onClose { fn() }
        }
    }

    override fun newSession(connectRequest: Request, sink: Websocket): SessionId {
        val sessionId = sessionProvider.assign(connectRequest)
        sessions[sessionId] = sink
        return sessionId
    }

    override fun start(executor: SimpleScheduler) {
        executor.scheduleWithFixedDelay({
            sessions.toList().forEach { (sessionId, sink) ->
                try {
                    sink.send(WsMessage(Event("ping", "").toMessage()))
                } catch (e: Exception) {
                    sessions.remove(sessionId)
                    sink.close()
                }
            }
        }, keepAliveDelay, keepAliveDelay)
    }
}
