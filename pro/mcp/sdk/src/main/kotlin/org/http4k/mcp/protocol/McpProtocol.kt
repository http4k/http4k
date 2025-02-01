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
import org.http4k.mcp.protocol.messages.fromJsonRpc
import org.http4k.mcp.protocol.messages.toJsonRpc
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
                    send(jsonReq.respondTo<McpInitialize.Request> { handleInitialize(it, sId) }, sId)

                McpCompletion.Method ->
                    send(jsonReq.respondTo<McpCompletion.Request> { completions.complete(it, req) }, sId)

                McpPing.Method -> send(jsonReq.respondTo<McpPing.Request> { Empty }, sId)

                McpPrompt.Get.Method ->
                    send(jsonReq.respondTo<McpPrompt.Get.Request> { prompts.get(it, req) }, sId)

                McpPrompt.List.Method ->
                    send(jsonReq.respondTo<McpPrompt.List.Request> { prompts.list(it, req) }, sId)

                McpResource.Template.List.Method ->
                    send(jsonReq.respondTo<McpResource.Template.List.Request> {
                        resources.listTemplates(it, req)
                    }, sId)

                McpResource.List.Method ->
                    send(jsonReq.respondTo<McpResource.List.Request> { resources.listResources(it, req) }, sId)

                McpResource.Read.Method ->
                    send(jsonReq.respondTo<McpResource.Read.Request> { resources.read(it, req) }, sId)

                McpResource.Subscribe.Method -> {
                    val subscribeRequest = jsonReq.fromJsonRpc<McpResource.Subscribe.Request>()
                    resources.subscribe(sId, subscribeRequest) {
                        send(
                            McpResource.Updated.Notification(subscribeRequest.uri).toJsonRpc(
                                McpResource.Updated
                            ),
                            sId
                        )
                    }
                    ok()
                }

                McpLogging.SetLevel.Method -> {
                    logger.setLevel(sId, jsonReq.fromJsonRpc<McpLogging.SetLevel.Request>().level)
                    ok()
                }

                McpResource.Unsubscribe.Method -> {
                    resources.unsubscribe(sId, jsonReq.fromJsonRpc())
                    ok()
                }

                McpInitialize.Initialized.Method -> ok()

                Cancelled.Method -> ok()

                McpSampling.Method ->
                    runCatching { jsonReq.fromJsonRpc<McpSampling.Request>() }
                        .map {
                            runCatching {
                                incomingSampling.sample(it, req)
                                    .forEach { send(it.toJsonRpc(jsonReq.id), sId) }
                                ok()
                            }.recover {
                                send(
                                    when (it) {
                                        is McpException -> it.error.toJsonRpc(jsonReq.id)
                                        else -> InternalError.toJsonRpc(jsonReq.id)
                                    }, sId
                                )
                                error()
                            }.getOrElse { error() }
                        }
                        .getOrElse {
                            send(InvalidRequest.toJsonRpc(jsonReq.id), sId)
                            error()
                        }

                McpProgress.Method -> ok()

                McpRoot.Changed.Method -> {
                    val requestId = RequestId.random(random)
                    clients[sId]?.addCall(requestId) { roots.update(it.fromJsonRpc()) }
                    send(McpRoot.List.Request().toJsonRpc(McpRoot.List, McpJson.asJsonObject(requestId)), sId)
                    ok()
                }

                McpTool.Call.Method ->
                    send(jsonReq.respondTo<McpTool.Call.Request> { tools.call(it, req) }, sId)

                McpTool.List.Method ->
                    send(jsonReq.respondTo<McpTool.List.Request> { tools.list(it, req) }, sId)

                else -> send(MethodNotFound.toJsonRpc(jsonReq.id), sId)
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
            send(
                McpLogging.LoggingMessage.Notification(level, logger, data).toJsonRpc(McpLogging.LoggingMessage),
                sId
            )
        }
        prompts.onChange(sId) {
            send(McpPrompt.List.Changed.Notification.toJsonRpc(McpPrompt.List.Changed), sId)
        }
        resources.onChange(sId) { send(McpResource.List.Changed.Notification.toJsonRpc(McpResource.List), sId) }
        tools.onChange(sId) { send(McpTool.List.Changed.Notification.toJsonRpc(McpTool.List.Changed), sId) }

        outgoingSampling.onRequest(sId, session.entity) { req, id ->
            clients[sId]?.addCall(id) { outgoingSampling.respond(session.entity, it.fromJsonRpc()) }
            send(req.toJsonRpc(McpSampling, McpJson.asJsonObject(id)), sId)
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

private inline fun <reified IN : ClientMessage.Request> JsonRpcRequest<McpNodeType>.respondTo(fn: (IN) -> ServerMessage.Response) =
    runCatching { fromJsonRpc<IN>() }
        .mapCatching(fn)
        .map { it.toJsonRpc(id) }
        .recover {
            when (it) {
                is McpException -> it.error.toJsonRpc(id)
                else -> InternalError.toJsonRpc(id)
            }
        }
        .getOrElse { InvalidRequest.toJsonRpc(id) }
