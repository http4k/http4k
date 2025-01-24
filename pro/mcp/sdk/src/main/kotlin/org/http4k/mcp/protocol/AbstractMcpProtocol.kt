package org.http4k.mcp.protocol

import org.http4k.core.Body
import org.http4k.core.Request
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
import org.http4k.mcp.model.LogLevel.error
import org.http4k.mcp.processing.McpMessageHandler
import org.http4k.mcp.processing.Serde
import org.http4k.mcp.protocol.ServerMessage.Response.Empty
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import org.http4k.sse.SseMessage
import kotlin.Long.Companion.MAX_VALUE
import kotlin.random.Random

abstract class AbstractMcpProtocol<RSP : Any>(
    private val metaData: ServerMetaData,
    private val tools: Tools,
    private val completions: Completions,
    private val resources: Resources,
    private val roots: Roots,
    private val sampling: Sampling,
    private val prompts: Prompts,
    private val logger: Logger,
    private val random: Random
) {
    private val serDe = Serde

    private val calls = mutableMapOf<MessageId, (JsonRpcResult<McpNodeType>) -> Unit>()

    protected abstract fun ok(): RSP
    protected abstract fun error(): RSP
    protected abstract fun send(message: SseMessage, sessionId: SessionId): RSP

    operator fun invoke(sId: SessionId, jsonReq: JsonRpcRequest<McpNodeType>, req: Request): RSP =
        when {
            jsonReq.valid() -> when (McpRpcMethod.of(jsonReq.method)) {
                McpInitialize.Method ->
                    send(
                        McpMessageHandler<McpInitialize.Request>(jsonReq) {
                            logger.subscribe(sId, error) { level, logger, data ->
                                send(McpMessageHandler(McpLogging.LoggingMessage(level, logger, data)), sId)
                            }
                            prompts.onChange(sId) { send(McpMessageHandler(McpPrompt.List.Changed), sId) }
                            resources.onChange(sId) { send(McpMessageHandler(McpResource.List.Changed), sId) }
                            tools.onChange(sId) { send(McpMessageHandler(McpTool.List.Changed), sId) }

                            onClose(sId) {
                                prompts.remove(sId)
                                resources.remove(sId)
                                tools.remove(sId)

                                logger.unsubscribe(sId)
                            }

                            McpInitialize.Response(metaData.entity, metaData.capabilities, metaData.protocolVersion)
                        }, sId
                    )

                McpCompletion.Method ->
                    send(McpMessageHandler<McpCompletion.Request>(jsonReq) { completions.complete(it, req) }, sId)

                McpPing.Method ->
                    send(McpMessageHandler<McpPing.Request>(jsonReq) { Empty }, sId)

                McpPrompt.Get.Method ->
                    send(McpMessageHandler<McpPrompt.Get.Request>(jsonReq) { prompts.get(it, req) }, sId)

                McpPrompt.List.Method ->
                    send(McpMessageHandler<McpPrompt.List.Request>(jsonReq) { prompts.list(it, req) }, sId)

                McpResource.Template.List.Method ->
                    send(McpMessageHandler<McpResource.Template.List.Request>(jsonReq) {
                        resources.listTemplates(it, req)
                    }, sId)

                McpResource.List.Method ->
                    send(McpMessageHandler<McpResource.List.Request>(jsonReq) { resources.listResources(it, req) }, sId)

                McpResource.Read.Method ->
                    send(McpMessageHandler<McpResource.Read.Request>(jsonReq) { resources.read(it, req) }, sId)

                McpResource.Subscribe.Method -> {
                    val subscribeRequest = serDe<McpResource.Subscribe.Request>(jsonReq)
                    resources.subscribe(sId, subscribeRequest) {
                        send(McpMessageHandler(McpResource.Updated(subscribeRequest.uri)), sId)
                    }
                    ok()
                }

                McpLogging.SetLevel.Method -> {
                    logger.setLevel(sId, serDe<McpLogging.SetLevel.Request>(jsonReq).level)
                    ok()
                }

                McpResource.Unsubscribe.Method -> {
                    resources.unsubscribe(sId, serDe(jsonReq))
                    ok()
                }

                McpInitialize.Initialized.Method -> ok()

                Cancelled.Method -> ok()

                McpSampling.Method -> send(
                    McpMessageHandler<McpSampling.Request>(jsonReq) { sampling.sample(it, req) },
                    sId
                )

                McpRoot.Changed.Method -> {
                    val messageId = MessageId.of(random.nextLong(0, MAX_VALUE))
                    calls[messageId] = { roots.update(serDe(it)) }
                    send(McpMessageHandler(McpRoot.List, McpRoot.List.Request(), McpJson.asJsonObject(messageId)), sId)
                    ok()
                }

                McpTool.Call.Method ->
                    send(McpMessageHandler<McpTool.Call.Request>(jsonReq) { tools.call(it, req) }, sId)

                McpTool.List.Method ->
                    send(McpMessageHandler<McpTool.List.Request>(jsonReq) { tools.list(it, req) }, sId)

                else -> error()
            }

            else -> {
                val result = Body.jsonRpcResult(McpJson).toLens()(req)

                when {
                    result.isError() -> error()
                    else -> with(McpJson) {
                        val id = result.id?.let { MessageId.parse(compact(it)) }
                        when (id) {
                            null -> ok()
                            else -> try {
                                calls[id]?.invoke(result)?.let { ok() } ?: error()
                            } finally {
                                calls -= id
                            }
                        }
                    }
                }
            }
        }

    abstract fun onClose(sessionId: SessionId, fn: () -> Unit)
}
