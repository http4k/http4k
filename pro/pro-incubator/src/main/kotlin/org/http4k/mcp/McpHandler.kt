package org.http4k.mcp

import com.fasterxml.jackson.databind.JsonNode
import dev.forkhandles.values.random
import org.http4k.connect.mcp.Cancelled
import org.http4k.connect.mcp.ClientMessage
import org.http4k.connect.mcp.Completion
import org.http4k.connect.mcp.HasMethod
import org.http4k.connect.mcp.Implementation
import org.http4k.connect.mcp.Initialize
import org.http4k.connect.mcp.McpRpcMethod
import org.http4k.connect.mcp.Ping
import org.http4k.connect.mcp.Prompt
import org.http4k.connect.mcp.ProtocolVersion
import org.http4k.connect.mcp.Resource
import org.http4k.connect.mcp.Root
import org.http4k.connect.mcp.ServerCapabilities
import org.http4k.connect.mcp.ServerMessage
import org.http4k.connect.mcp.ServerMessage.Response.Empty
import org.http4k.connect.mcp.Tool
import org.http4k.connect.mcp.util.McpJson
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.filter.debug
import org.http4k.format.jsonRpcRequest
import org.http4k.format.jsonRpcResult
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage.Event
import org.http4k.routing.bind as httpBind

fun McpHandler(
    implementation: Implementation,
    protocolVersion: ProtocolVersion,
    capabilities: ServerCapabilities,
    tools: Tools,
    resources: Resources,
    prompts: Prompts,
    roots: Roots,
    completions: Completions,
    newSessionId: () -> SessionId = { SessionId.random() }
): PolyHandler {
    val json = McpJson

    val serDe = Serde(json)

    fun initialise(req: Initialize.Request) = Initialize.Response(capabilities, implementation, protocolVersion)

    val sessions = mutableMapOf<SessionId, Sse>()
    val calls = mutableMapOf<MessageId, (JsonRpcResult<JsonNode>) -> Unit>()

    return poly(
        "/sse" bind sse {
            val sessionId = newSessionId()
            sessions[sessionId] = it
            it.send(Event("endpoint", Uri.of("/message").query("sessionId", sessionId.value.toString()).toString()))
            it.onClose {
                tools.remove(sessionId)
                resources.remove(sessionId)
                prompts.remove(sessionId)
                roots.remove(sessionId)
                sessions.remove(sessionId)
            }
        }.debug(),
        routes(
            "/message" httpBind POST to { req: Request ->
                val sId = SessionId.parse(req.query("sessionId")!!)

                val request = Body.jsonRpcRequest(json).toLens()(req)

                if (request.valid()) {
                    when (McpRpcMethod.of(request.method)) {
                        Initialize.Method -> sessions[sId].respondTo(serDe, Initialize, request, ::initialise)

                        Completion.Method -> sessions[sId].respondTo(serDe, Completion, request, completions::complete)

                        Ping.Method -> sessions[sId].send(serDe, Ping, Empty, request.id)
                        Prompt.Get.Method -> sessions[sId].respondTo(serDe, Prompt.Get, request, prompts::get)
                        Prompt.List.Method -> sessions[sId].respondTo(serDe, Prompt.List, request, prompts::list)

                        Resource.List.Method -> sessions[sId].respondTo(serDe, Resource.List, request, resources::list)
                        Resource.Read.Method -> sessions[sId].respondTo(serDe, Resource.Read, request, resources::read)

                        Resource.Subscribe.Method -> {
                            resources.subscribe(serDe(request))
                            Response(ACCEPTED)
                        }

                        Resource.Unsubscribe.Method -> {
                            resources.unsubscribe(serDe(request))
                            Response(ACCEPTED)
                        }

                        Initialize.Notification.Method -> Response(ACCEPTED)
                        Cancelled.Notification.Method -> Response(ACCEPTED)

                        Root.Notification.Method -> {
                            val messageId = MessageId.random()
                            calls[messageId] = { roots.update(serDe(it)) }
                            sessions[sId].send(serDe, Root.List, Root.List.Request(), json.asJsonObject(messageId))
                        }

                        Tool.Call.Method -> sessions[sId].respondTo(serDe, Tool.Call, request, tools::call)
                        Tool.List.Method -> sessions[sId].respondTo(serDe, Tool.List, request, tools::list)

                        else -> Response(NOT_IMPLEMENTED)
                    }
                } else {
                    val result = Body.jsonRpcResult(json).toLens()(req)

                    with(McpJson) {
                        val messageId = asA<MessageId>(asFormatString(result.id ?: nullNode()))
                        try {
                            calls[messageId]?.invoke(result)
                        } finally {
                            calls -= messageId
                        }
                    }
                    Response(ACCEPTED)
                }

            }
        )
    )
}

fun <NODE : Any> Sse?.send(
    serDe: Serde<NODE>,
    hasMethod: HasMethod,
    resp: ServerMessage,
    id: NODE? = null
): Response {
    when (this) {
        null -> Unit
        else -> send(serDe(hasMethod.Method, resp, id))
    }
    return Response(ACCEPTED)
}

private inline fun <reified IN : ClientMessage.Request, OUT : ServerMessage.Response, NODE : Any>
    Sse?.respondTo(serDe: Serde<NODE>, hasMethod: HasMethod, req: JsonRpcRequest<NODE>, fn: (IN) -> OUT): Response {
    when (this) {
        null -> Response(BAD_REQUEST)
        else -> runCatching { serDe<IN>(req) }
            .onFailure {
                send(serDe(InvalidRequest, req.id))
                return Response(BAD_REQUEST)
            }
            .map(fn)
            .map {
                send(serDe(hasMethod.Method, it, req.id))
                return Response(ACCEPTED)
            }
            .recover {
                send(serDe(InternalError, req.id))
                return Response(SERVICE_UNAVAILABLE)
            }
    }
    return Response(NOT_IMPLEMENTED)
}
