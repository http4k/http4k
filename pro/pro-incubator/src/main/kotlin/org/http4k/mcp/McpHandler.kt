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
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
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
import org.http4k.routing.bind as hbind

fun McpHandler(
    implementation: Implementation,
    protocolVersion: ProtocolVersion,
    capabilities: ServerCapabilities,
    tools: Tools,
    resources: Resources,
    prompts: Prompts
): PolyHandler {
    val serDe = Serde(McpJson)

    val roots = Roots(emptyList())

    val sessions = mutableMapOf<SessionId, Sse>()


    val initial = Initialize.Response(capabilities, implementation, protocolVersion)
    return poly(
        "/sse" bind sse {
            val newSessionId = SessionId.random()

            sessions[newSessionId] = it
            it.send(Event("endpoint", Uri.of("/message").query("sessionId", newSessionId.toString()).toString()))
            it.send(serDe(initial, McpJson.number(0)))
        },
        routes(
            "/message" hbind POST to { req: Request ->
                val request = Body.jsonRpcRequest(McpJson).toLens()(req)
                val sessionId = SessionId.parse(req.query("sessionId")!!)
                System.err.println(request)

                when (McpRpcMethod.of(request.method)) {
                    Initialize.Method -> serDe.message(request, { _: Initialize.Request -> initial })
                    Initialize.Notification.Method -> serDe.message(request, { _: Initialize.Notification.Request -> Empty })
                    Cancelled.Notification.Method -> serDe.message(request, { _: Cancelled.Notification.Request -> Empty })

                    Ping.Method -> serDe.message(request, { _: Ping.Request -> Empty })
                    Prompt.Get.Method -> serDe.message(request, prompts::get)
                    Prompt.List.Method -> {
                        sessions[sessionId]?.send(serDe(prompts.list(serDe<Prompt.List.Request>(request)), request.id))
                        Response(ACCEPTED)
                    }

                    Resource.List.Method -> serDe.message(request, resources::list)
                    Resource.Read.Method -> serDe.message(request, resources::read)
                    Resource.Subscribe.Method -> serDe.message(request, resources::subscribe)
                    Resource.Unsubscribe.Method -> serDe.message(request, resources::unsubscribe)

                    Root.List.Method -> serDe.message(request, roots::list)
                    Root.Notification.Method -> serDe.message(request, { _: Root.Notification.Request -> Empty })
                    Tool.Call.Method -> serDe.message(request, tools::call)
                    Tool.List.Method -> serDe.message(request, tools::list)

                    else -> Response(NOT_IMPLEMENTED)
                }
            }
        ).debug()
    )
}

private inline fun <reified IN : ClientRequest, OUT : ServerResponse, NODE : Any>
    Serde<NODE>.message(req: JsonRpcRequest<NODE>, fn: (IN) -> OUT): Response {
    runCatching { this<IN>(req) }
        .onFailure { return Response(BAD_REQUEST).body(this(InvalidRequest, req.id).toMessage()) }
        .map { fn(it) }
        .map { return Response(ACCEPTED).body(this(it, req.id).toMessage()) }
        .recover { return Response(SERVICE_UNAVAILABLE).body(this(InternalError, req.id).toMessage()) }
    return Response(INTERNAL_SERVER_ERROR)
}
