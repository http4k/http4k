package org.http4k.mcp

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.connect.mcp.ClientRequest
import org.http4k.connect.mcp.Complete
import org.http4k.connect.mcp.Implementation
import org.http4k.connect.mcp.Initialize
import org.http4k.connect.mcp.McpRpcMethod
import org.http4k.connect.mcp.Ping
import org.http4k.connect.mcp.Prompt
import org.http4k.connect.mcp.ProtocolVersion
import org.http4k.connect.mcp.Resource
import org.http4k.connect.mcp.ServerCapabilities
import org.http4k.connect.mcp.ServerResponse
import org.http4k.connect.mcp.Tool
import org.http4k.connect.mcp.util.McpJson
import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.format.jsonRpcRequest
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.sse.Sse
import org.http4k.sse.SseHandler
import org.http4k.sse.SseResponse

class McpHandler(
    private val implementation: Implementation,
    private val protocolVersion: ProtocolVersion,
    private val capabilities: ServerCapabilities,
    private val tools: Tools,
    private val resources: Resources,
    private val prompts: Prompts
) : SseHandler {
    private val serDe = Serde(McpJson)

    override fun invoke(p1: Request): SseResponse = SseResponse {
        val rpcRequest = Body.jsonRpcRequest(McpJson).toLens()(it.connectRequest)

        when (McpRpcMethod.of(rpcRequest.method)) {
            Initialize.Method -> it.respondTo(rpcRequest, ::initialize)
            Ping.Method -> it.respondTo(rpcRequest, ::ping)

            Complete.Method -> it.respondTo(rpcRequest, ::complete)

            Prompt.Get.Method -> it.respondTo(rpcRequest, prompts::get)
            Prompt.List.Method -> it.respondTo(rpcRequest, prompts::list)

            Resource.List.Method -> it.respondTo(rpcRequest, resources::list)
            Resource.Read.Method -> it.respondTo(rpcRequest, resources::read)
            Resource.Subscribe.Method -> it.respondTo(rpcRequest, resources::subscribe)
            Resource.Unsubscribe.Method -> it.respondTo(rpcRequest, resources::unsubscribe)

            Tool.Call.Method -> it.respondTo(rpcRequest, tools::call)
            Tool.List.Method -> it.respondTo(rpcRequest, tools::list)

            else -> it.send(serDe(MethodNotFound))
        }
    }

    private fun complete(req: Complete.Request): Complete.Response {
        TODO()
    }

    private fun ping(input: Ping.Request) = ServerResponse.Empty

    private fun initialize(req: Initialize.Request) =
        Initialize.Response(capabilities, implementation, protocolVersion)

    private inline fun <reified IN : ClientRequest, OUT : ServerResponse>
        Sse.respondTo(req: JsonRpcRequest<JsonNode>, fn: (IN) -> OUT) {
        runCatching { serDe<IN>(req) }
            .onFailure { send(serDe(InvalidRequest)) }
            .map { fn(it) }
            .onSuccess { send(serDe(it)) }
            .onFailure { send(serDe(InternalError)) }
    }
}
