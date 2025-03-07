package org.http4k.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.format.MoshiNode
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.CompletionStatus.Finished
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
import org.http4k.mcp.server.capability.CompletionCapability
import org.http4k.mcp.server.capability.PromptCapability
import org.http4k.mcp.server.capability.ResourceCapability
import org.http4k.mcp.server.capability.ServerCapability
import org.http4k.mcp.server.capability.ServerCompletions
import org.http4k.mcp.server.capability.ServerPrompts
import org.http4k.mcp.server.capability.ServerResources
import org.http4k.mcp.server.capability.ServerRoots
import org.http4k.mcp.server.capability.ServerSampling
import org.http4k.mcp.server.capability.ServerTools
import org.http4k.mcp.server.capability.ToolCapability
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Models the MCP protocol in terms of message handling and session management.
 */
class McpProtocol<Transport, RSP : Any>(
    internal val metaData: ServerMetaData,
    private val clientSessions: ClientSessions<Transport, RSP>,
    private val tools: Tools = ServerTools(),
    private val resources: Resources = ServerResources(),
    private val prompts: Prompts = ServerPrompts(),
    private val completions: Completions = ServerCompletions(),
    private val sampling: Sampling = ServerSampling(),
    private val logger: Logger = ServerLogger(),
    private val roots: Roots = ServerRoots(),
    private val random: Random = Random
) {
    constructor(
        serverMetaData: ServerMetaData,
        clientSessions: ClientSessions<Transport, RSP>,
        vararg capabilities: ServerCapability
    ) : this(
        serverMetaData,
        clientSessions,
        ServerTools(capabilities.flatMap { it }.filterIsInstance<ToolCapability>()),
        ServerResources(capabilities.flatMap { it }.filterIsInstance<ResourceCapability>()),
        ServerPrompts(capabilities.flatMap { it }.filterIsInstance<PromptCapability>()),
        ServerCompletions(capabilities.flatMap { it }.filterIsInstance<CompletionCapability>()),
    )

    private val clients = ConcurrentHashMap<SessionId, ClientRequestTracking>()

    fun receive(sId: SessionId, httpReq: Request): RSP {
        val payload = runCatching {
            McpJson.fields(McpJson.parse(httpReq.bodyString())).toMap()
        }.getOrElse { return clientSessions.error() }

        return when {
            payload["method"] != null -> {
                val jsonReq = JsonRpcRequest(McpJson, payload)

                when (McpRpcMethod.of(jsonReq.method)) {
                    McpInitialize.Method ->
                        clientSessions.send(sId, jsonReq.respondTo<McpInitialize.Request> { handleInitialize(it, sId) })

                    McpCompletion.Method ->
                        clientSessions.send(
                            sId,
                            jsonReq.respondTo<McpCompletion.Request> { completions.complete(it, httpReq) }
                        )

                    McpPing.Method -> clientSessions.send(
                        sId,
                        jsonReq.respondTo<McpPing.Request> { ServerMessage.Response.Empty }
                    )

                    McpPrompt.Get.Method ->
                        clientSessions.send(sId, jsonReq.respondTo<McpPrompt.Get.Request> { prompts.get(it, httpReq) })

                    McpPrompt.List.Method ->
                        clientSessions.send(
                            sId,
                            jsonReq.respondTo<McpPrompt.List.Request> { prompts.list(it, httpReq) })

                    McpResource.Template.List.Method ->
                        clientSessions.send(sId, jsonReq.respondTo<McpResource.Template.List.Request> {
                            resources.listTemplates(it, httpReq)
                        })

                    McpResource.List.Method ->
                        clientSessions.send(sId, jsonReq.respondTo<McpResource.List.Request> {
                            resources.listResources(
                                it,
                                httpReq
                            )
                        })

                    McpResource.Read.Method ->
                        clientSessions.send(
                            sId,
                            jsonReq.respondTo<McpResource.Read.Request> { resources.read(it, httpReq) })

                    McpResource.Subscribe.Method -> {
                        val subscribeRequest = jsonReq.fromJsonRpc<McpResource.Subscribe.Request>()
                        resources.subscribe(sId, subscribeRequest) {
                            clientSessions.send(
                                sId,
                                McpResource.Updated.Notification(subscribeRequest.uri).toJsonRpc(McpResource.Updated)
                            )
                        }
                        clientSessions.ok()
                    }

                    McpLogging.SetLevel.Method -> {
                        logger.setLevel(sId, jsonReq.fromJsonRpc<McpLogging.SetLevel.Request>().level)
                        clientSessions.ok()
                    }

                    McpResource.Unsubscribe.Method -> {
                        resources.unsubscribe(sId, jsonReq.fromJsonRpc())
                        clientSessions.ok()
                    }

                    McpInitialize.Initialized.Method -> clientSessions.ok()

                    Cancelled.Method -> clientSessions.ok()

                    McpProgress.Method -> clientSessions.ok()

                    McpRoot.Changed.Method -> {
                        val requestId = RequestId.random(random)
                        clients[sId]?.trackRequest(requestId) { roots.update(it.fromJsonRpc()) }
                        clientSessions.send(
                            sId,
                            McpRoot.List.Request().toJsonRpc(McpRoot.List, McpJson.asJsonObject(requestId))
                        )
                        clientSessions.ok()
                    }

                    McpTool.Call.Method -> clientSessions.send(
                        sId,
                        jsonReq.respondTo<McpTool.Call.Request> { tools.call(it, httpReq) }
                    )

                    McpTool.List.Method -> clientSessions.send(
                        sId,
                        jsonReq.respondTo<McpTool.List.Request> { tools.list(it, httpReq) }
                    )

                    else -> clientSessions.send(sId, ErrorMessage.MethodNotFound.toJsonRpc(jsonReq.id))
                }
            }

            else -> {
                val jsonResult = JsonRpcResult(McpJson, payload)

                when {
                    jsonResult.isError() -> clientSessions.ok()
                    else -> with(McpJson) {
                        val id = jsonResult.id?.let { RequestId.parse(compact(it)) }
                        when (id) {
                            null -> clientSessions.ok()
                            else -> clients[sId]?.processResult(id, jsonResult)?.let { clientSessions.ok() }
                                ?: clientSessions.error()
                        }
                    }
                }
            }
        }
    }

    fun handleInitialize(request: McpInitialize.Request, sId: SessionId): McpInitialize.Response {
        val session = ClientRequestTracking()

        clients[sId] = session
        logger.subscribe(sId, LogLevel.error) { level, logger, data ->
            clientSessions.send(
                sId,
                McpLogging.LoggingMessage.Notification(level, logger, data).toJsonRpc(McpLogging.LoggingMessage)
            )
        }
        prompts.onChange(sId) {
            clientSessions.send(sId, McpPrompt.List.Changed.Notification.toJsonRpc(McpPrompt.List.Changed))
        }
        resources.onChange(sId) {
            clientSessions.send(
                sId,
                McpResource.List.Changed.Notification.toJsonRpc(McpResource.List.Changed)
            )
        }
        tools.onChange(sId) {
            clientSessions.send(
                sId,
                McpTool.List.Changed.Notification.toJsonRpc(McpTool.List.Changed)
            )
        }

        sampling.onSampleClient(sId, request.clientInfo.name) { req, id ->
            clients[sId]?.trackRequest(id) { sampling.receive(id, it.fromJsonRpc()) }
            clientSessions.send(sId, req.toJsonRpc(McpSampling, McpJson.asJsonObject(id)))
        }

        clientSessions.onClose(sId) {
            clients.remove(sId)
            prompts.remove(sId)
            resources.remove(sId)
            tools.remove(sId)
            sampling.remove(sId)
            logger.unsubscribe(sId)
        }

        return McpInitialize.Response(metaData.entity, metaData.capabilities, metaData.protocolVersion)
    }

    fun newSession(req: Request, sink: Transport) = clientSessions.new(req, sink)


    private class ClientRequestTracking {
        private val calls = ConcurrentHashMap<RequestId, (JsonRpcResult<McpNodeType>) -> CompletionStatus>()

        fun trackRequest(id: RequestId, callback: (JsonRpcResult<McpNodeType>) -> CompletionStatus) {
            calls[id] = callback
        }

        fun processResult(id: RequestId, result: JsonRpcResult<MoshiNode>) {
            val done = calls[id]?.invoke(result) ?: Finished
            if (done == Finished) calls.remove(id)
        }
    }
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
