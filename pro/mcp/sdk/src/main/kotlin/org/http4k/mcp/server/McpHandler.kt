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
import org.http4k.mcp.protocol.ServerMessage.Response.Empty
import org.http4k.mcp.util.McpJson
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
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


                fun initialise(req: McpInitialize.Request, http: Request) =
                    McpInitialize.Response(metaData.entity, metaData.capabilities, metaData.protocolVersion)

                val sendBlock: (SseMessage) -> Response = sessions[sId]::send
                val errorBlock: () -> Response = { Response(GONE) }
                val unitBlock: (Unit) -> Response = { Response(ACCEPTED) }

                val jsonReq = Body.jsonRpcRequest(json).toLens()(req)

                if (jsonReq.valid()) {
                    when (McpRpcMethod.of(jsonReq.method)) {
                        McpInitialize.Method ->
                            handler<McpInitialize.Request>(jsonReq) { initialise(it, req) }
                                .let(sendBlock)

                        McpCompletion.Method ->
                            handler<McpCompletion.Request>(jsonReq) { completions.complete(it, req) }
                                .let(sendBlock)

                        McpPing.Method ->
                            handler<McpPing.Request>(jsonReq) { Empty }
                                .let(sendBlock)

                        McpPrompt.Get.Method ->
                            handler<McpPrompt.Get.Request>(jsonReq) { prompts.get(it, req) }
                                .let(sendBlock)

                        McpPrompt.List.Method ->
                            handler<McpPrompt.List.Request>(jsonReq) { prompts.list(it, req) }
                                .let(sendBlock)

                        McpResource.Template.List.Method ->
                            handler<McpResource.Template.List.Request>(jsonReq) { resources.listTemplates(it, req) }
                                .let(sendBlock)

                        McpResource.List.Method ->
                            handler<McpResource.List.Request>(jsonReq) { resources.listResources(it, req) }
                                .let(sendBlock)

                        McpResource.Read.Method ->
                            handler<McpResource.Read.Request>(jsonReq) { resources.read(it, req) }
                                .let(sendBlock)

                        McpResource.Subscribe.Method -> {
                            val subscribeRequest = serDe<McpResource.Subscribe.Request>(jsonReq)
                            resources.subscribe(sId, subscribeRequest) {
                                sessions[sId]?.send(handler(McpResource.Updated(subscribeRequest.uri)))
                            }.let(unitBlock)
                        }

                        McpLogging.SetLevel.Method ->
                            logger.setLevel(sId, serDe<McpLogging.SetLevel.Request>(jsonReq).level)
                                .let(unitBlock)

                        McpResource.Unsubscribe.Method ->
                            resources.unsubscribe(sId, serDe(jsonReq)).let(unitBlock)

                        McpInitialize.Initialized.Method -> unitBlock(Unit)

                        Cancelled.Method -> unitBlock(Unit)

                        McpSampling.Method -> handler<McpSampling.Request>(jsonReq) { sampling.sample(it, req) }
                            .let(sendBlock)

                        McpRoot.Changed.Method -> {
                            val messageId = MessageId.random(random)
                            calls[messageId] = { roots.update(serDe(it)) }
                            sessions[sId]?.send(
                                handler(McpRoot.List, McpRoot.List.Request(), json.asJsonObject(messageId))
                            )
                            unitBlock(Unit)
                        }

                        McpTool.Call.Method ->
                            handler<McpTool.Call.Request>(jsonReq) { tools.call(it, req) }
                                .let(sendBlock)

                        McpTool.List.Method ->
                            handler<McpTool.List.Request>(jsonReq) { tools.list(it, req) }
                                .let(sendBlock)

                        else -> errorBlock()
                    }
                } else {
                    val result = Body.jsonRpcResult(json).toLens()(req)

                    when {
                        result.isError() -> errorBlock()
                        else -> with(McpJson) {
                            val messageId = MessageId.parse(asFormatString(result.id ?: nullNode()))
                            try {
                                calls[messageId]?.invoke(result)?.let { unitBlock(Unit) } ?: errorBlock()
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

private fun Sse?.send(message: SseMessage) = when (this) {
    null -> Response(GONE)
    else -> Response(ACCEPTED).also { send(message) }
}
