package org.http4k.mcp.server

import com.fasterxml.jackson.databind.JsonNode
import dev.forkhandles.values.random
import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.core.Response
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
import org.http4k.mcp.util.McpJson
import org.http4k.sse.SseMessage
import kotlin.random.Random

abstract class McpProtocolLogic(
    private val metaData: ServerMetaData,
    private val tools: Tools,
    private val completions: Completions,
    private val resources: Resources,
    private val roots: Roots,
    private val sampling: Sampling,
    private val handler: McpMessageHandler<JsonNode>,
    private val prompts: Prompts,
    private val logger: Logger,
    private val random: Random,
    private val json: McpJson
) {
    private val serDe = Serde(json)

    private val calls = mutableMapOf<MessageId, (JsonRpcResult<JsonNode>) -> Unit>()

    protected abstract fun unit(unit: Unit): Response
    protected abstract fun send(message: SseMessage, sessionId: SessionId): Response
    protected abstract fun error(): Response

    operator fun invoke(sId: SessionId, jsonReq: JsonRpcRequest<JsonNode>, req: Request): Response =
        if (jsonReq.valid()) {
            when (McpRpcMethod.of(jsonReq.method)) {
                McpInitialize.Method ->
                    handler<McpInitialize.Request>(jsonReq) {
                        McpInitialize.Response(metaData.entity, metaData.capabilities, metaData.protocolVersion)
                    }
                        .let { send(it, sId) }

                McpCompletion.Method ->
                    send(handler<McpCompletion.Request>(jsonReq) { completions.complete(it, req) }, sId)

                McpPing.Method ->
                    send(handler<McpPing.Request>(jsonReq) { ServerMessage.Response.Empty }, sId)

                McpPrompt.Get.Method ->
                    send(handler<McpPrompt.Get.Request>(jsonReq) { prompts.get(it, req) }, sId)

                McpPrompt.List.Method ->
                    send(handler<McpPrompt.List.Request>(jsonReq) { prompts.list(it, req) }, sId)

                McpResource.Template.List.Method ->
                    send(handler<McpResource.Template.List.Request>(jsonReq) { resources.listTemplates(it, req) }, sId)

                McpResource.List.Method ->
                    send(handler<McpResource.List.Request>(jsonReq) { resources.listResources(it, req) }, sId)

                McpResource.Read.Method ->
                    send(handler<McpResource.Read.Request>(jsonReq) { resources.read(it, req) }, sId)

                McpResource.Subscribe.Method -> {
                    val subscribeRequest = serDe<McpResource.Subscribe.Request>(jsonReq)
                    resources.subscribe(sId, subscribeRequest) {
                        send(handler(McpResource.Updated(subscribeRequest.uri)), sId)
                    }.let(::unit)
                }

                McpLogging.SetLevel.Method ->
                    logger.setLevel(sId, serDe<McpLogging.SetLevel.Request>(jsonReq).level)
                        .let(::unit)

                McpResource.Unsubscribe.Method ->
                    resources.unsubscribe(sId, serDe(jsonReq)).let(::unit)

                McpInitialize.Initialized.Method -> unit(Unit)

                Cancelled.Method -> unit(Unit)

                McpSampling.Method -> send(handler<McpSampling.Request>(jsonReq) { sampling.sample(it, req) }, sId)

                McpRoot.Changed.Method -> {
                    val messageId = MessageId.random(random)
                    calls[messageId] = { roots.update(serDe(it)) }
                    send(handler(McpRoot.List, McpRoot.List.Request(), json.asJsonObject(messageId)), sId)
                    unit(Unit)
                }

                McpTool.Call.Method ->
                    send(handler<McpTool.Call.Request>(jsonReq) { tools.call(it, req) }, sId)

                McpTool.List.Method ->
                    send(handler<McpTool.List.Request>(jsonReq) { tools.list(it, req) }, sId)

                else -> error()
            }
        } else {
            val result = Body.jsonRpcResult(json).toLens()(req)

            when {
                result.isError() -> error()
                else -> with(McpJson) {
                    val messageId = MessageId.parse(asFormatString(result.id ?: nullNode()))
                    try {
                        calls[messageId]?.invoke(result)?.let { unit(Unit) } ?: error()
                    } finally {
                        calls -= messageId
                    }
                }
            }
        }
}
