package org.http4k.mcp.protocol

import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.format.jsonRpcResult
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
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
import org.http4k.mcp.processing.fromJsonRpc
import org.http4k.mcp.processing.toJsonRpc
import org.http4k.mcp.protocol.messages.Cancelled
import org.http4k.mcp.protocol.messages.ClientMessage
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
import org.http4k.mcp.protocol.messages.ServerMessage
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
                    send(McpJson.respondTo<McpInitialize.Request>(jsonReq) { handleInitialize(it, sId) }, sId)

                McpCompletion.Method ->
                    send(McpJson.respondTo<McpCompletion.Request>(jsonReq) { completions.complete(it, req) }, sId)

                McpPing.Method -> send(McpJson.respondTo<McpPing.Request>(jsonReq) { Empty }, sId)

                McpPrompt.Get.Method ->
                    send(McpJson.respondTo<McpPrompt.Get.Request>(jsonReq) { prompts.get(it, req) }, sId)

                McpPrompt.List.Method ->
                    send(McpJson.respondTo<McpPrompt.List.Request>(jsonReq) { prompts.list(it, req) }, sId)

                McpResource.Template.List.Method ->
                    send(McpJson.respondTo<McpResource.Template.List.Request>(jsonReq) {
                        resources.listTemplates(it, req)
                    }, sId)

                McpResource.List.Method ->
                    send(McpJson.respondTo<McpResource.List.Request>(jsonReq) { resources.listResources(it, req) }, sId)

                McpResource.Read.Method ->
                    send(McpJson.respondTo<McpResource.Read.Request>(jsonReq) { resources.read(it, req) }, sId)

                McpResource.Subscribe.Method -> {
                    val subscribeRequest = McpJson.fromJsonRpc<McpResource.Subscribe.Request>(jsonReq)
                    resources.subscribe(sId, subscribeRequest) {
                        send(
                            McpJson.toJsonRpc(McpResource.Updated, McpResource.Updated.Notification(subscribeRequest.uri)),
                            sId
                        )
                    }
                    ok()
                }

                McpLogging.SetLevel.Method -> {
                    logger.setLevel(sId, McpJson.fromJsonRpc<McpLogging.SetLevel.Request>(jsonReq).level)
                    ok()
                }

                McpResource.Unsubscribe.Method -> {
                    resources.unsubscribe(sId, McpJson.fromJsonRpc(jsonReq))
                    ok()
                }

                McpInitialize.Initialized.Method -> ok()

                Cancelled.Method -> ok()

                McpSampling.Method -> {
                    val requestId = McpJson.asA(jsonReq.id ?: McpJson.nullNode(), RequestId::class)
                    send(
                        McpJson.respondTo<McpSampling.Request>(jsonReq) {
                            incomingSampling.sample(it, requestId, req)
                        },
                        sId
                    )
                }

                McpProgress.Method -> ok()

                McpRoot.Changed.Method -> {
                    val requestId = RequestId.random(random)
                    clients[sId]?.addCall(requestId) { roots.update(McpJson.fromJsonRpc(it)) }
                    send(McpJson.toJsonRpc(McpRoot.List, McpRoot.List.Request(), McpJson.asJsonObject(requestId)), sId)
                    ok()
                }

                McpTool.Call.Method ->
                    send(McpJson.respondTo<McpTool.Call.Request>(jsonReq) { tools.call(it, req) }, sId)

                McpTool.List.Method ->
                    send(McpJson.respondTo<McpTool.List.Request>(jsonReq) { tools.list(it, req) }, sId)

                else -> send(McpJson.toJsonRpc(MethodNotFound, jsonReq.id), sId)
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

    private fun handleInitialize(request: McpInitialize.Request, sId: SessionId) =
        with(McpJson) {
            val session = ClientSession(request.clientInfo, request.capabilities)

            clients[sId] = session
            logger.subscribe(sId, error) { level, logger, data ->
                send(
                    toJsonRpc(McpLogging.LoggingMessage, McpLogging.LoggingMessage.Notification(level, logger, data)),
                    sId
                )
            }
            prompts.onChange(sId) { send(toJsonRpc(McpPrompt.List.Changed, McpPrompt.List.Changed.Notification), sId) }
            resources.onChange(sId) { send(toJsonRpc(McpResource.List, McpResource.List.Changed.Notification), sId) }
            tools.onChange(sId) { send(toJsonRpc(McpTool.List.Changed, McpTool.List.Changed.Notification), sId) }

            outgoingSampling.onRequest(sId, session.entity) { req, requestId ->
                clients[sId]?.addCall(requestId) { outgoingSampling.respond(session.entity, fromJsonRpc(it)) }
                send(toJsonRpc(McpSampling, req, McpJson.asJsonObject(requestId)), sId)
            }

            onClose(sId) {
                clients.remove(sId)
                prompts.remove(sId)
                resources.remove(sId)
                tools.remove(sId)
                outgoingSampling.remove(sId, session.entity)
                logger.unsubscribe(sId)
            }

            McpInitialize.Response(metaData.entity, metaData.capabilities, metaData.protocolVersion)
        }


    protected abstract fun onClose(sessionId: SessionId, fn: () -> Unit)

    /**
     * Start the protocol.
     */
    abstract fun start(executor: SimpleScheduler = SimpleSchedulerService(1)): Future<*>
}

private inline fun <reified IN : ClientMessage.Request> McpJson.respondTo(
    req: JsonRpcRequest<McpNodeType>,
    fn: (IN) -> ServerMessage.Response
) =
    runCatching { fromJsonRpc<IN>(req) }
        .mapCatching(fn)
        .map { toJsonRpc(it, req.id) }
        .recover {
            when (it) {
                is McpException -> toJsonRpc(it.error, req.id)
                else -> toJsonRpc(InternalError, req.id)
            }
        }
        .getOrElse { toJsonRpc(InvalidRequest, req.id) }
