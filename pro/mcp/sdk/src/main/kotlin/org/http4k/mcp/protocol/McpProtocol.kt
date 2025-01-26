package org.http4k.mcp.protocol

import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.format.jsonRpcResult
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

abstract class McpProtocol<RSP : Any>(
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
    private val calls = ConcurrentHashMap<MessageId, (JsonRpcResult<McpNodeType>) -> Unit>()

    protected abstract fun ok(): RSP
    protected abstract fun error(): RSP
    protected abstract fun send(message: SseMessage.Event, sessionId: SessionId): RSP

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
                    val subscribeRequest = Serde<McpResource.Subscribe.Request>(jsonReq)
                    resources.subscribe(sId, subscribeRequest) {
                        send(McpMessageHandler(McpResource.Updated(subscribeRequest.uri)), sId)
                    }
                    ok()
                }

                McpLogging.SetLevel.Method -> {
                    logger.setLevel(sId, Serde<McpLogging.SetLevel.Request>(jsonReq).level)
                    ok()
                }

                McpResource.Unsubscribe.Method -> {
                    resources.unsubscribe(sId, Serde(jsonReq))
                    ok()
                }

                McpInitialize.Initialized.Method -> ok()

                Cancelled.Method -> ok()

                McpSampling.Method -> send(
                    McpMessageHandler<McpSampling.Request>(jsonReq) { sampling.sample(it, req) },
                    sId
                )

                McpProgress.Notification.Method -> ok()

                McpRoot.Changed.Method -> {
                    val messageId = MessageId.of(random.nextLong(0, MAX_MCP_MESSAGE_ID))
                    calls[messageId] = { roots.update(Serde(it)) }
                    send(McpMessageHandler(McpRoot.List, McpRoot.List.Request(), McpJson.asJsonObject(messageId)), sId)
                    ok()
                }

                McpTool.Call.Method ->
                    send(McpMessageHandler<McpTool.Call.Request>(jsonReq) { tools.call(it, req) }, sId)

                McpTool.List.Method ->
                    send(McpMessageHandler<McpTool.List.Request>(jsonReq) { tools.list(it, req) }, sId)

                else -> send(Serde(MethodNotFound, jsonReq.id), sId)
            }

            else -> {
                val result = Body.jsonRpcResult(McpJson).toLens()(req)
                when {
                    result.isError() -> ok()
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

/**
 * This is the maximum Integer value that can be represented precisely by raw JSON number when
 * Moshi deserializes it as a double. MCP servers seem to need a precise integer value for the
 * message ID, so we need to limit the range of the message ID to this value.
 */
private const val MAX_MCP_MESSAGE_ID = 9_007_199_254_740_991L
