package org.http4k.mcp.protocol

import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.format.jsonRpcResult
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.mcp.capability.Completions
import org.http4k.mcp.capability.IncomingSampling
import org.http4k.mcp.capability.Logger
import org.http4k.mcp.capability.OutgoingSampling
import org.http4k.mcp.capability.Prompts
import org.http4k.mcp.capability.Resources
import org.http4k.mcp.capability.Roots
import org.http4k.mcp.capability.Tools
import org.http4k.mcp.model.LogLevel.error
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.processing.McpMessageHandler
import org.http4k.mcp.processing.SerDe
import org.http4k.mcp.protocol.messages.Cancelled
import org.http4k.mcp.protocol.messages.McpCompletion
import org.http4k.mcp.protocol.messages.McpInitialize
import org.http4k.mcp.protocol.messages.McpLogging
import org.http4k.mcp.protocol.messages.McpPing
import org.http4k.mcp.protocol.messages.McpProgress
import org.http4k.mcp.protocol.messages.McpPrompt
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.protocol.messages.McpRoot
import org.http4k.mcp.protocol.messages.McpSampling
import org.http4k.mcp.protocol.messages.McpTool
import org.http4k.mcp.protocol.messages.ServerMessage.Response.Empty
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import kotlin.random.Random

/**
 * Models the MCP protocol in terms of message handling and session management.
 */
abstract class McpProtocol<RSP : Any>(
    private val metaData: ServerMetaData,
    private val tools: Tools,
    private val completions: Completions,
    private val resources: Resources,
    private val roots: Roots,
    private val incomingSampling: IncomingSampling,
    private val outgoingSampling: OutgoingSampling,
    private val prompts: Prompts,
    private val logger: Logger,
    private val random: Random
) {
    private val clients = ConcurrentHashMap<SessionId, ClientSession>()

    protected abstract fun ok(): RSP
    protected abstract fun error(): RSP
    protected abstract fun send(message: McpNodeType, sessionId: SessionId): RSP

    operator fun invoke(sId: SessionId, jsonReq: JsonRpcRequest<McpNodeType>, req: Request): RSP =
        when {
            jsonReq.valid() -> when (McpRpcMethod.of(jsonReq.method)) {
                McpInitialize.Method ->
                    send(McpMessageHandler<McpInitialize.Request>(jsonReq) { handleInitialize(it, sId) }, sId)

                McpCompletion.Method ->
                    send(McpMessageHandler<McpCompletion.Request>(jsonReq) { completions.complete(it, req) }, sId)

                McpPing.Method -> send(McpMessageHandler<McpPing.Request>(jsonReq) { Empty }, sId)

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
                    val subscribeRequest = SerDe<McpResource.Subscribe.Request>(jsonReq)
                    resources.subscribe(sId, subscribeRequest) {
                        send(McpMessageHandler(McpResource.Updated, McpResource.Updated.Notification(subscribeRequest.uri)), sId)
                    }
                    ok()
                }

                McpLogging.SetLevel.Method -> {
                    logger.setLevel(sId, SerDe<McpLogging.SetLevel.Request>(jsonReq).level)
                    ok()
                }

                McpResource.Unsubscribe.Method -> {
                    resources.unsubscribe(sId, SerDe(jsonReq))
                    ok()
                }

                McpInitialize.Initialized.Method -> ok()

                Cancelled.Method -> ok()

                McpSampling.Method -> {
                    val requestId = McpJson.asA(jsonReq.id ?: McpJson.nullNode(), RequestId::class)
                    send(
                        McpMessageHandler<McpSampling.Request>(jsonReq) {
                            incomingSampling.sample(it, requestId, req)
                        },
                        sId
                    )
                }

                McpProgress.Method -> ok()

                McpRoot.Changed.Method -> {
                    val requestId = RequestId.random(random)
                    clients[sId]?.addCall(requestId) { roots.update(SerDe(it)) }
                    send(McpMessageHandler(McpRoot.List, McpRoot.List.Request(), McpJson.asJsonObject(requestId)), sId)
                    ok()
                }

                McpTool.Call.Method ->
                    send(McpMessageHandler<McpTool.Call.Request>(jsonReq) { tools.call(it, req) }, sId)

                McpTool.List.Method ->
                    send(McpMessageHandler<McpTool.List.Request>(jsonReq) { tools.list(it, req) }, sId)

                else -> send(SerDe(MethodNotFound, jsonReq.id), sId)
            }

            else -> {
                val result = Body.jsonRpcResult(McpJson).toLens()(req)
                when {
                    result.isError() -> ok()
                    else -> with(McpJson) {
                        val id = result.id?.let { RequestId.parse(compact(it)) }
                        when (id) {
                            null -> ok()
                            else -> clients[sId]?.processResult(id, result)?.let { ok() } ?: error()
                        }
                    }
                }
            }
        }

    private fun handleInitialize(request: McpInitialize.Request, sId: SessionId): McpInitialize.Response {
        val session = ClientSession(request.clientInfo, request.capabilities)

        clients[sId] = session
        logger.subscribe(sId, error) { level, logger, data ->
            send(McpMessageHandler(McpLogging.LoggingMessage, McpLogging.LoggingMessage.Notification(level, logger, data)), sId)
        }
        prompts.onChange(sId) { send(McpMessageHandler(McpPrompt.List.Changed, McpPrompt.List.Changed.Notification), sId) }
        resources.onChange(sId) { send(McpMessageHandler(McpResource.List, McpResource.List.Changed.Notification), sId) }
        tools.onChange(sId) { send(McpMessageHandler(McpTool.List.Changed, McpTool.List.Changed.Notification), sId) }

        outgoingSampling.onRequest(sId, session.entity) { req, requestId ->
            clients[sId]?.addCall(requestId) { outgoingSampling.respond(session.entity, SerDe(it)) }
            send(McpMessageHandler(McpSampling, req, McpJson.asJsonObject(requestId)), sId)
        }

        onClose(sId) {
            clients.remove(sId)
            prompts.remove(sId)
            resources.remove(sId)
            tools.remove(sId)
            outgoingSampling.remove(sId, session.entity)
            logger.unsubscribe(sId)
        }

        return McpInitialize.Response(metaData.entity, metaData.capabilities, metaData.protocolVersion)
    }

    protected abstract fun onClose(sessionId: SessionId, fn: () -> Unit)

    /**
     * Start the protocol.
     */
    abstract fun start(executor: SimpleScheduler = SimpleSchedulerService(1)): Future<*>
}
