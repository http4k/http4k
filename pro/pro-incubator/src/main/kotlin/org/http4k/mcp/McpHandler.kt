package org.http4k.mcp

import dev.forkhandles.values.random
import org.http4k.connect.mcp.Cancelled
import org.http4k.connect.mcp.ClientRequest
import org.http4k.connect.mcp.Completetion
import org.http4k.connect.mcp.Implementation
import org.http4k.connect.mcp.Initialize
import org.http4k.connect.mcp.Logging
import org.http4k.connect.mcp.McpRpcMethod
import org.http4k.connect.mcp.Ping
import org.http4k.connect.mcp.Progress
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
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.format.jsonRpcRequest
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.routing.RoutingSseHandler
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage.Event

fun McpHandler(
    implementation: Implementation,
    protocolVersion: ProtocolVersion,
    capabilities: ServerCapabilities,
    tools: Tools,
    resources: Resources,
    prompts: Prompts
): RoutingSseHandler {
    val serDe = Serde(McpJson)

    val roots = Roots(emptyList())

    val sessions = mutableMapOf<SessionId, Unit>()
    fun complete(req: Completetion.Request): Completetion.Response {
        TODO()
    }

    fun ping(input: Ping.Request) = Empty

    fun initialize(req: Initialize.Request) =
        Initialize.Response(capabilities, implementation, protocolVersion)

    return sse(
        "/sse" bind sse {
            val newSessionId = SessionId.random()

            sessions[newSessionId] = Unit

            it.send(Event("endpoint", Uri.of("/message").query("sessionId", newSessionId.toString()).toString()))
            it.send(serDe(Initialize.Response(capabilities, implementation, protocolVersion)))
        },
        "/message" bind sse {
            val rpcRequest = Body.jsonRpcRequest(McpJson).toLens()(it.connectRequest)

            when (McpRpcMethod.of(rpcRequest.method)) {
                Initialize.Method -> it.respondTo(serDe, rpcRequest, ::initialize)
                Initialize.Notification.Method -> it.respondTo(
                    serDe,
                    rpcRequest,
                    { _: Initialize.Notification.Request -> Empty })

                Ping.Method -> it.respondTo(serDe, rpcRequest, ::ping)
                Cancelled.Notification.Method -> it.respondTo(
                    serDe,
                    rpcRequest,
                    { _: Cancelled.Notification.Request -> Empty })

                Completetion.Method -> it.respondTo(serDe, rpcRequest, ::complete)
                Logging.SetLevel.Method -> it.respondTo(serDe, rpcRequest, resources::list)
                Progress.Notification.Method -> it.respondTo(
                    serDe,
                    rpcRequest,
                    { _: Progress.Notification.Request -> Empty })

                Prompt.Get.Method -> it.respondTo(serDe, rpcRequest, prompts::get)
                Prompt.List.Method -> it.respondTo(serDe, rpcRequest, prompts::list)

                Resource.List.Method -> it.respondTo(serDe, rpcRequest, resources::list)
                Resource.Read.Method -> it.respondTo(serDe, rpcRequest, resources::read)
                Resource.Subscribe.Method -> it.respondTo(serDe, rpcRequest, resources::subscribe)
                Resource.Unsubscribe.Method -> it.respondTo(serDe, rpcRequest, resources::unsubscribe)

                Root.List.Method -> it.respondTo(serDe, rpcRequest, roots::list)
                Root.Notification.Method -> it.respondTo(serDe, rpcRequest, { _: Root.Notification.Request -> Empty })

                Tool.Call.Method -> it.respondTo(serDe, rpcRequest, tools::call)
                Tool.List.Method -> it.respondTo(serDe, rpcRequest, tools::list)

                else -> it.send(serDe(MethodNotFound))
            }
        }
    )
}

private inline fun <reified IN : ClientRequest, OUT : ServerResponse, NODE : Any>
    Sse.respondTo(serDe: Serde<NODE>, req: JsonRpcRequest<NODE>, fn: (IN) -> OUT) {
    runCatching { serDe<IN>(req) }
        .onFailure { send(serDe(InvalidRequest)) }
        .map { fn(it) }
        .onSuccess { send(serDe(it)) }
        .onFailure { send(serDe(InternalError)) }
}
