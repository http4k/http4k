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
import org.http4k.connect.mcp.McpTool
import org.http4k.connect.mcp.Ping
import org.http4k.connect.mcp.Prompt
import org.http4k.connect.mcp.ProtocolVersion
import org.http4k.connect.mcp.Resource
import org.http4k.connect.mcp.Root
import org.http4k.connect.mcp.ServerCapabilities
import org.http4k.connect.mcp.ServerMessage
import org.http4k.connect.mcp.ServerMessage.Response.Empty
import org.http4k.connect.mcp.util.McpJson
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.GONE
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.format.jsonRpcRequest
import org.http4k.format.jsonRpcResult
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import kotlin.random.Random
import org.http4k.routing.bind as httpBind

fun McpHandler(
    implementation: Implementation,
    protocolVersion: ProtocolVersion,
    capabilities: ServerCapabilities,
    completions: Completions,
    roots: Roots,
    tools: McpTools,
    resources: Resources,
    resourceTemplates: ResourceTemplates,
    prompts: Prompts,
    random: Random = Random
): PolyHandler {
    val json = McpJson

    val serDe = Serde(json)

    fun initialise(req: Initialize.Request) = Initialize.Response(capabilities, implementation, protocolVersion)

    val sessions = Sessions(serDe, tools, resources, prompts, random)
    val calls = mutableMapOf<MessageId, (JsonRpcResult<JsonNode>) -> Unit>()

    return poly(
        "/sse" bind sse {
            sessions.add(it)
        },
        routes(
            "/message" httpBind POST to { req: Request ->
                val sId = SessionId.parse(req.query("sessionId")!!)

                val jsonReq = Body.jsonRpcRequest(json).toLens()(req)

                if (jsonReq.valid()) {
                    when (McpRpcMethod.of(jsonReq.method)) {
                        Initialize.Method -> sessions[sId].respondTo(Initialize, jsonReq, ::initialise)

                        Completion.Method -> sessions[sId].respondTo(Completion, jsonReq, completions::complete)

                        Ping.Method -> sessions[sId].respondTo(Ping, jsonReq, { _: Ping.Request -> Empty })
                        Prompt.Get.Method -> sessions[sId].respondTo(Prompt.Get, jsonReq, prompts::get)
                        Prompt.List.Method -> sessions[sId].respondTo(Prompt.List, jsonReq, prompts::list)

                        Resource.Template.List.Method -> sessions[sId].respondTo(
                            Resource.Template.List,
                            jsonReq,
                            resourceTemplates::list
                        )

                        Resource.List.Method -> sessions[sId].respondTo(Resource.List, jsonReq, resources::list)
                        Resource.Read.Method -> sessions[sId].respondTo(Resource.Read, jsonReq, resources::read)

                        Resource.Subscribe.Method -> {
                            val req1 = serDe<Resource.Subscribe.Request>(jsonReq)
                            resources.subscribe(sId, req1) { sessions[sId]?.send(Resource.Updated(req1.uri)) }
                            Response(ACCEPTED)
                        }

                        Resource.Unsubscribe.Method -> {
                            resources.unsubscribe(sId, serDe(jsonReq))
                            Response(ACCEPTED)
                        }

                        Initialize.Initialized.Method -> Response(ACCEPTED)
                        Cancelled.Method -> Response(ACCEPTED)

                        Root.Changed.Method -> {
                            val messageId = MessageId.random(random)
                            calls[messageId] = { roots.update(serDe(it)) }
                            sessions[sId]?.send(Root.List, Root.List.Request(), json.asJsonObject(messageId))
                            Response(ACCEPTED)
                        }

                        McpTool.Call.Method -> sessions[sId].respondTo(McpTool.Call, jsonReq) { call: McpTool.Call.Request ->
                            tools.call(call, req)
                        }

                        McpTool.List.Method -> sessions[sId].respondTo(McpTool.List, jsonReq, tools::list)

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

private inline fun <reified IN : ClientMessage.Request, OUT : ServerMessage.Response, NODE : Any>
    Session<NODE>?.respondTo(hasMethod: HasMethod, req: JsonRpcRequest<NODE>, fn: (IN) -> OUT) =
    when (this) {
        null -> Response(GONE)
        else -> {
            process(req, fn)
            Response(ACCEPTED)
        }
    }
