package org.http4k.mcp

import dev.forkhandles.values.random
import org.http4k.connect.mcp.Cancelled
import org.http4k.connect.mcp.ClientRequest
import org.http4k.connect.mcp.Implementation
import org.http4k.connect.mcp.Initialize
import org.http4k.connect.mcp.McpRpcMethod
import org.http4k.connect.mcp.Ping
import org.http4k.connect.mcp.Prompt
import org.http4k.connect.mcp.ProtocolVersion
import org.http4k.connect.mcp.Resource
import org.http4k.connect.mcp.Root
import org.http4k.connect.mcp.Sampling
import org.http4k.connect.mcp.ServerCapabilities
import org.http4k.connect.mcp.ServerResponse
import org.http4k.connect.mcp.ServerResponse.Empty
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
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.jsonrpc.JsonRpcRequest
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
    newSessionId: () -> SessionId = { SessionId.random() }
): PolyHandler {
    val serDe = Serde(McpJson)

    val roots = Roots(emptyList())

    fun initialise(req: Initialize.Request) = Initialize.Response(capabilities, implementation, protocolVersion)

    val sessions = mutableMapOf<SessionId, Sse>()

    return poly(
        "/sse" bind sse {
            val sessionId = newSessionId()
            sessions[sessionId] = it
            it.send(Event("endpoint", Uri.of("/message").query("sessionId", sessionId.value.toString()).toString()))
        },
        routes(
            "/message" httpBind POST to { req: Request ->
                val request = Body.jsonRpcRequest(McpJson).toLens()(req)
                val sId = SessionId.parse(req.query("sessionId")!!)
                System.err.println(request)

                when (McpRpcMethod.of(request.method)) {
                    Initialize.Method -> sessions[sId].respondTo(serDe, request, ::initialise)
                    Initialize.Notification.Method -> Response(ACCEPTED)
                    Cancelled.Notification.Method -> Response(ACCEPTED)
                    Ping.Method -> sessions[sId].respondTo(serDe, request) { _: Ping.Request -> Empty }
                    Prompt.Get.Method -> sessions[sId].respondTo(serDe, request, prompts::get)
                    Prompt.List.Method -> sessions[sId].respondTo(serDe, request, prompts::list)

                    Resource.List.Method -> sessions[sId].respondTo(serDe, request, resources::list)
                    Resource.Read.Method -> sessions[sId].respondTo(serDe, request, resources::read)
                    Resource.Subscribe.Method -> sessions[sId].respondTo(serDe, request, resources::subscribe)
                    Resource.Unsubscribe.Method -> sessions[sId].respondTo(serDe, request, resources::unsubscribe)

                    Root.List.Method -> sessions[sId].respondTo(serDe, request, roots::list)
                    Root.Notification.Method -> Response(ACCEPTED)
                    Sampling.Message.Create.Method -> Response(ACCEPTED)

                    Tool.Call.Method -> sessions[sId].respondTo(serDe, request, tools::call)
                    Tool.List.Method -> sessions[sId].respondTo(serDe, request, tools::list)

                    else -> Response(NOT_IMPLEMENTED)
                }
            }
        ).debug()
    )

}

private inline fun <reified IN : ClientRequest, OUT : ServerResponse, NODE : Any>
    Sse?.respondTo(serDe: Serde<NODE>, req: JsonRpcRequest<NODE>, fn: (IN) -> OUT): Response {
    when (this) {
        null -> Response(BAD_REQUEST)
        else -> runCatching { serDe<IN>(req) }
            .onFailure {
                send(serDe(InvalidRequest, req.id))
                return Response(BAD_REQUEST)
            }
            .map(fn)
            .map {
                send(serDe(it, req.id))
                return Response(ACCEPTED)
            }
            .recover {
                send(serDe(InternalError, req.id))
                return Response(SERVICE_UNAVAILABLE)
            }
    }
    return Response(NOT_IMPLEMENTED)
}
