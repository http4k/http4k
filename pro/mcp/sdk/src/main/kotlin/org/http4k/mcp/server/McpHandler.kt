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
import org.http4k.format.jsonRpcRequest
import org.http4k.format.jsonRpcResult
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.mcp.features.Completions
import org.http4k.mcp.features.Logger
import org.http4k.mcp.features.Prompts
import org.http4k.mcp.features.Resources
import org.http4k.mcp.features.Roots
import org.http4k.mcp.features.Sampling
import org.http4k.mcp.features.Tools
import org.http4k.mcp.processing.McpMessageHandler
import org.http4k.mcp.processing.Serde
import org.http4k.mcp.protocol.Cancelled
import org.http4k.mcp.protocol.ClientMessage
import org.http4k.mcp.protocol.McpCompletion
import org.http4k.mcp.protocol.McpInitialize
import org.http4k.mcp.protocol.McpLogging
import org.http4k.mcp.protocol.McpPing
import org.http4k.mcp.protocol.McpPrompt
import org.http4k.mcp.protocol.McpResource
import org.http4k.mcp.protocol.McpRoot
import org.http4k.mcp.protocol.McpRpcMethod
import org.http4k.mcp.protocol.McpSampling
import org.http4k.mcp.protocol.McpTool
import org.http4k.mcp.protocol.MessageId
import org.http4k.mcp.protocol.ServerMessage
import org.http4k.mcp.protocol.ServerMessage.Response.Empty
import org.http4k.mcp.util.McpJson
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.Sse
import kotlin.random.Random
import org.http4k.routing.bind as httpBind

/**
 * This is the main entry point for the MCP server. It handles the various MCP messages on both HTTP and SSE.
 */
fun McpHandler(
    metaData: ServerMetaData,
    prompts: Prompts = Prompts(emptyList()),
    tools: Tools = Tools(emptyList()),
    resources: Resources = Resources(emptyList()),
    completions: Completions = Completions(emptyList()),
    sampling: Sampling = Sampling(emptyList()),
    roots: Roots = Roots(),
    logger: Logger = Logger(),
    random: Random = Random
): PolyHandler {
    val json = McpJson

    val serDe = Serde(json)

    fun initialise(req: McpInitialize.Request, http: Request) =
        McpInitialize.Response(metaData.entity, metaData.capabilities, metaData.protocolVersion)

    val handler = McpMessageHandler(json)
    val sessions = ClientSessions(tools, resources, prompts, logger, random, handler)
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
                        McpInitialize.Method ->
                            sessions[sId].respond<McpInitialize.Request, JsonNode>(handler, jsonReq) {
                                initialise(it, req)
                            }

                        McpCompletion.Method ->
                            sessions[sId].respond<McpCompletion.Request, JsonNode>(
                                handler,
                                jsonReq
                            ) { completions.complete(it, req) }

                        McpPing.Method ->
                            sessions[sId].respond<McpPing.Request, JsonNode>(handler, jsonReq) { Empty }

                        McpPrompt.Get.Method ->
                            sessions[sId].respond<McpPrompt.Get.Request, JsonNode>(handler, jsonReq) {
                                prompts.get(it, req)
                            }

                        McpPrompt.List.Method ->
                            sessions[sId].respond<McpPrompt.List.Request, JsonNode>(handler, jsonReq) {
                                prompts.list(it, req)
                            }

                        McpResource.Template.List.Method ->
                            sessions[sId].respond<McpResource.Template.List.Request, JsonNode>(
                                handler,
                                jsonReq
                            ) { resources.listTemplates(it, req) }

                        McpResource.List.Method ->
                            sessions[sId].respond<McpResource.List.Request, JsonNode>(
                                handler,
                                jsonReq
                            ) { resources.listResources(it, req) }

                        McpResource.Read.Method ->
                            sessions[sId].respond<McpResource.Read.Request, JsonNode>(
                                handler,
                                jsonReq
                            ) { resources.read(it, req) }

                        McpResource.Subscribe.Method -> {
                            val subscribeRequest = serDe<McpResource.Subscribe.Request>(jsonReq)
                            resources.subscribe(sId, subscribeRequest) {
                                sessions[sId]?.send(handler(McpResource.Updated(subscribeRequest.uri)))
                            }
                            Response(ACCEPTED)
                        }

                        McpLogging.SetLevel.Method -> {
                            logger.setLevel(sId, serDe<McpLogging.SetLevel.Request>(jsonReq).level)
                            Response(ACCEPTED)
                        }

                        McpResource.Unsubscribe.Method -> {
                            resources.unsubscribe(sId, serDe(jsonReq))
                            Response(ACCEPTED)
                        }

                        McpInitialize.Initialized.Method -> Response(ACCEPTED)
                        Cancelled.Method -> Response(ACCEPTED)

                        McpSampling.Method ->
                            sessions[sId].respond<McpSampling.Request, JsonNode>(handler, jsonReq) {
                                sampling.sample(
                                    it,
                                    req
                                )
                            }

                        McpRoot.Changed.Method -> {
                            val messageId = MessageId.random(random)
                            calls[messageId] = { roots.update(serDe(it)) }
                            sessions[sId]?.send(
                                handler(McpRoot.List, McpRoot.List.Request(), json.asJsonObject(messageId))
                            )
                            Response(ACCEPTED)
                        }

                        McpTool.Call.Method ->
                            sessions[sId].respond<McpTool.Call.Request, JsonNode>(handler, jsonReq) {
                                tools.call(
                                    it,
                                    req
                                )
                            }

                        McpTool.List.Method ->
                            sessions[sId].respond<McpTool.List.Request, JsonNode>(handler, jsonReq) {
                                tools.list(
                                    it,
                                    req
                                )
                            }

                        else -> Response(GONE)
                    }
                } else {
                    val result = Body.jsonRpcResult(json).toLens()(req)

                    when {
                        result.isError() -> Response(GONE)
                        else -> with(McpJson) {
                            val messageId = MessageId.parse(asFormatString(result.id ?: nullNode()))
                            try {
                                calls[messageId]?.invoke(result)?.let { Response(ACCEPTED) } ?: Response(GONE)
                            } finally {
                                calls -= messageId
                            }
                        }
                    }
                }
            }
        )
    )
}

private inline fun <reified Req : ClientMessage.Request, NODE : Any> Sse?.respond(
    handler: McpMessageHandler<NODE>, jsonReq: JsonRpcRequest<NODE>, fn: (Req) -> ServerMessage.Response
) = when (this) {
    null -> Response(GONE)
    else -> Response(ACCEPTED).also { send(handler<Req>(jsonReq, fn)) }
}
