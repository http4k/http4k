package org.http4k.mcp.server.protocol

import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.mcp.model.LogLevel
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.McpException
import org.http4k.mcp.protocol.McpRpcMethod
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.SessionId
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
import org.http4k.mcp.protocol.messages.fromJsonRpc
import org.http4k.mcp.protocol.messages.toJsonRpc
import org.http4k.mcp.server.capability.Completions
import org.http4k.mcp.server.capability.Logger
import org.http4k.mcp.server.capability.Prompts
import org.http4k.mcp.server.capability.Resources
import org.http4k.mcp.server.capability.Roots
import org.http4k.mcp.server.capability.Sampling
import org.http4k.mcp.server.capability.Tools
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
    private val sampling: Sampling,
    private val prompts: Prompts,
    private val logger: Logger,
    private val random: Random
) {
    private val clients = ConcurrentHashMap<SessionId, ClientSession>()

    protected abstract fun ok(): RSP
    protected abstract fun error(): RSP
    protected abstract fun send(message: McpNodeType, sessionId: SessionId): RSP

    operator fun invoke(sId: SessionId, httpReq: Request): RSP {
        val payload = McpJson.fields(McpJson.parse(httpReq.bodyString())).toMap()

        return when {
            payload["method"] != null -> {
                val jsonReq = JsonRpcRequest(McpJson, payload)

                when (McpRpcMethod.of(jsonReq.method)) {
                    McpInitialize.Method ->
                        send(jsonReq.respondTo<McpInitialize.Request> { handleInitialize(it, sId) }, sId)

                    McpCompletion.Method ->
                        send(jsonReq.respondTo<McpCompletion.Request> { completions.complete(it, httpReq) }, sId)

                    McpPing.Method -> send(jsonReq.respondTo<McpPing.Request> { ServerMessage.Response.Empty }, sId)

                    McpPrompt.Get.Method ->
                        send(jsonReq.respondTo<McpPrompt.Get.Request> { prompts.get(it, httpReq) }, sId)

                    McpPrompt.List.Method ->
                        send(jsonReq.respondTo<McpPrompt.List.Request> { prompts.list(it, httpReq) }, sId)

                    McpResource.Template.List.Method ->
                        send(jsonReq.respondTo<McpResource.Template.List.Request> {
                            resources.listTemplates(it, httpReq)
                        }, sId)

                    McpResource.List.Method ->
                        send(jsonReq.respondTo<McpResource.List.Request> { resources.listResources(it, httpReq) }, sId)

                    McpResource.Read.Method ->
                        send(jsonReq.respondTo<McpResource.Read.Request> { resources.read(it, httpReq) }, sId)

                    McpResource.Subscribe.Method -> {
                        val subscribeRequest = jsonReq.fromJsonRpc<McpResource.Subscribe.Request>()
                        resources.subscribe(sId, subscribeRequest) {
                            send(
                                McpResource.Updated.Notification(subscribeRequest.uri).toJsonRpc(McpResource.Updated),
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
                                    sampling.sampleServer(it, httpReq)
                                        .forEach { send(it.toJsonRpc(jsonReq.id), sId) }
                                    ok()
                                }.recover {
                                    send(
                                        when (it) {
                                            is McpException -> it.error
                                            else -> InternalError
                                        }.toJsonRpc(jsonReq.id), sId
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

                    McpTool.Call.Method -> send(
                        jsonReq.respondTo<McpTool.Call.Request> { tools.call(it, httpReq) },
                        sId
                    )

                    McpTool.List.Method -> send(
                        jsonReq.respondTo<McpTool.List.Request> { tools.list(it, httpReq) },
                        sId
                    )

                    else -> send(ErrorMessage.MethodNotFound.toJsonRpc(jsonReq.id), sId)
                }
            }

            else -> {
                val jsonResult = JsonRpcResult(McpJson, payload)

                when {
                    jsonResult.isError() -> ok()
                    else -> with(McpJson) {
                        val id = jsonResult.id?.let { RequestId.parse(compact(it)) }
                        when (id) {
                            null -> ok()
                            else -> clients[sId]?.processResult(id, jsonResult)?.let { ok() } ?: error()
                        }
                    }
                }
            }
        }
    }

    private fun handleInitialize(request: McpInitialize.Request, sId: SessionId): McpInitialize.Response {
        val session = ClientSession(request.clientInfo, request.capabilities)

        clients[sId] = session
        logger.subscribe(sId, LogLevel.error) { level, logger, data ->
            send(
                McpLogging.LoggingMessage.Notification(level, logger, data).toJsonRpc(McpLogging.LoggingMessage),
                sId
            )
        }
        prompts.onChange(sId) {
            send(McpPrompt.List.Changed.Notification.toJsonRpc(McpPrompt.List.Changed), sId)
        }
        resources.onChange(sId) { send(McpResource.List.Changed.Notification.toJsonRpc(McpResource.List.Changed), sId) }
        tools.onChange(sId) { send(McpTool.List.Changed.Notification.toJsonRpc(McpTool.List.Changed), sId) }

        sampling.onSampleClient(sId, request.clientInfo.name) { req, id ->
            clients[sId]?.addCall(id) { sampling.receive(id, it.fromJsonRpc()) }
            send(req.toJsonRpc(McpSampling, McpJson.asJsonObject(id)), sId)
        }

        onClose(sId) {
            clients.remove(sId)
            prompts.remove(sId)
            resources.remove(sId)
            tools.remove(sId)
            sampling.remove(request.clientInfo.name, sId)
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
