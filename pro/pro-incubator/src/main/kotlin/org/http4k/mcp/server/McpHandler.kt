package org.http4k.mcp.server

import com.fasterxml.jackson.databind.JsonNode
import dev.forkhandles.values.random
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
import org.http4k.mcp.protocol.Cancelled
import org.http4k.mcp.protocol.ClientMessage
import org.http4k.mcp.protocol.Implementation
import org.http4k.mcp.protocol.McpInitialize
import org.http4k.mcp.protocol.McpPing
import org.http4k.mcp.protocol.McpPrompt
import org.http4k.mcp.protocol.McpResource
import org.http4k.mcp.protocol.McpRoot
import org.http4k.mcp.protocol.McpRpcMethod
import org.http4k.mcp.protocol.McpTool
import org.http4k.mcp.protocol.MessageId
import org.http4k.mcp.protocol.ProtocolVersion
import org.http4k.mcp.protocol.ServerCapabilities
import org.http4k.mcp.protocol.ServerMessage
import org.http4k.mcp.protocol.ServerMessage.Response.Empty
import org.http4k.mcp.util.McpJson
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
    completions: McpCompletions,
    roots: McpRoots,
    tools: McpTools,
    resources: McpResources,
    resourceTemplates: McpResourceTemplates,
    prompts: McpPrompts,
    random: Random = Random
): PolyHandler {
    val json = McpJson

    val serDe = Serde(json)

    fun initialise(req: McpInitialize.Request) = McpInitialize.Response(capabilities, implementation, protocolVersion)

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
                        McpInitialize.Method -> sessions[sId].respondTo(jsonReq, ::initialise)

                        _root_ide_package_.org.http4k.mcp.protocol.McpCompletion.Method -> sessions[sId].respondTo(jsonReq, completions::complete)

                        McpPing.Method -> sessions[sId].respondTo(jsonReq) { _: McpPing.Request -> Empty }
                        McpPrompt.Get.Method -> sessions[sId].respondTo(jsonReq) { call: McpPrompt.Get.Request ->
                            prompts.get(call, req)
                        }

                        McpPrompt.List.Method -> sessions[sId].respondTo(jsonReq, prompts::list)

                        McpResource.Template.List.Method -> sessions[sId].respondTo(
                            jsonReq,
                            resourceTemplates::list
                        )

                        McpResource.List.Method -> sessions[sId].respondTo(jsonReq, resources::list)
                        McpResource.Read.Method -> sessions[sId].respondTo(jsonReq, resources::read)

                        McpResource.Subscribe.Method -> {
                            val req1 = serDe<McpResource.Subscribe.Request>(jsonReq)
                            resources.subscribe(sId, req1) { sessions[sId]?.send(McpResource.Updated(req1.uri)) }
                            Response(ACCEPTED)
                        }

                        McpResource.Unsubscribe.Method -> {
                            resources.unsubscribe(sId, serDe(jsonReq))
                            Response(ACCEPTED)
                        }

                        McpInitialize.Initialized.Method -> Response(ACCEPTED)
                        Cancelled.Method -> Response(ACCEPTED)

                        McpRoot.Changed.Method -> {
                            val messageId = MessageId.random(random)
                            calls[messageId] = { roots.update(serDe(it)) }
                            sessions[sId]?.send(McpRoot.List, McpRoot.List.Request(), json.asJsonObject(messageId))
                            Response(ACCEPTED)
                        }

                        McpTool.Call.Method -> sessions[sId].respondTo(jsonReq) { call: McpTool.Call.Request ->
                            tools.call(call, req)
                        }

                        McpTool.List.Method -> sessions[sId].respondTo(jsonReq, tools::list)

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
    Session<NODE>?.respondTo(req: JsonRpcRequest<NODE>, fn: (IN) -> OUT) =
    when (this) {
        null -> Response(GONE)
        else -> {
            process(req, fn)
            Response(ACCEPTED)
        }
    }
