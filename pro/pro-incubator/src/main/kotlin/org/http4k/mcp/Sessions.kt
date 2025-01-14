package org.http4k.mcp

import dev.forkhandles.values.random
import org.http4k.connect.mcp.ClientMessage
import org.http4k.connect.mcp.HasMethod
import org.http4k.connect.mcp.Prompt
import org.http4k.connect.mcp.Resource
import org.http4k.connect.mcp.ServerMessage
import org.http4k.connect.mcp.Tool
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage.Event
import kotlin.random.Random

class Sessions<NODE : Any>(
    val serDe: Serde<NODE>,
    private val tools: Tools,
    private val resources: Resources,
    private val prompts: Prompts,
    private val random: Random
) {
    private val sessions = mutableMapOf<SessionId, Sse>()

    fun add(sse: Sse) {
        val sessionId = SessionId.random(random)
        prompts.onChange(sessionId) { send(sessionId, Prompt.List, Prompt.List.Notification) }
        resources.onChange(sessionId) { send(sessionId, Resource.List, Resource.List.Notification) }
        tools.onChange(sessionId) { send(sessionId, Tool.List, Tool.List.Notification) }

        sessions[sessionId] = sse

        sse.onClose {
            prompts.remove(sessionId)
            resources.remove(sessionId)
            tools.remove(sessionId)

            sessions.remove(sessionId)
        }
        sse.send(Event("endpoint", Uri.of("/message").query("sessionId", sessionId.value.toString()).toString()))
    }

    inline fun <reified IN : ClientMessage.Request, OUT : ServerMessage.Response>
        respondTo(sessionId: SessionId, hasMethod: HasMethod, req: JsonRpcRequest<NODE>, fn: (IN) -> OUT): Response {
        when (val sse = this[sessionId]) {
            null -> Response(BAD_REQUEST)
            else -> runCatching { serDe<IN>(req) }
                .onFailure {
                    sse.send(serDe(InvalidRequest, req.id))
                    return Response(BAD_REQUEST)
                }
                .map(fn)
                .map {
                    sse.send(serDe(hasMethod.Method, it, req.id))
                    return Response(ACCEPTED)
                }
                .recover {
                    sse.send(serDe(InternalError, req.id))
                    return Response(SERVICE_UNAVAILABLE)
                }
        }
        return Response(NOT_IMPLEMENTED)
    }

    fun send(
        sessionId: SessionId,
        hasMethod: HasMethod,
        resp: ServerMessage,
        id: NODE? = null
    ): Response {
        when (val sse = this[sessionId]) {
            null -> Unit
            else -> sse.send(serDe(hasMethod.Method, resp, id))
        }
        return Response(ACCEPTED)
    }
    operator fun get(sessionId: SessionId) = sessions[sessionId]
}
