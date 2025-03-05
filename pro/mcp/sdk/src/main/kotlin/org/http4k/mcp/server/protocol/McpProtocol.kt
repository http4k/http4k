package org.http4k.mcp.server.protocol

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
import org.http4k.mcp.server.capability.CompletionCapability
import org.http4k.mcp.server.capability.Completions
import org.http4k.mcp.server.capability.Logger
import org.http4k.mcp.server.capability.PromptCapability
import org.http4k.mcp.server.capability.Prompts
import org.http4k.mcp.server.capability.ResourceCapability
import org.http4k.mcp.server.capability.Resources
import org.http4k.mcp.server.capability.Roots
import org.http4k.mcp.server.capability.Sampling
import org.http4k.mcp.server.capability.ServerCapability
import org.http4k.mcp.server.capability.ToolCapability
import org.http4k.mcp.server.capability.Tools
import org.http4k.mcp.server.session.McpConnection
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Models the MCP protocol in terms of message handling and session management.
 */
open class McpProtocol<RSP : Any>(
    private val metaData: ServerMetaData,
    private val tools: Tools = Tools(),
    private val resources: Resources = Resources(),
    private val prompts: Prompts = Prompts(),
    private val completions: Completions = Completions(),
    private val roots: Roots = Roots(),
    private val sampling: Sampling = Sampling(),
    private val logger: Logger = Logger(),
    private val random: Random = Random
) {
    constructor(
        serverMetaData: ServerMetaData,
        capabilities: Array<out ServerCapability>
    ) : this(
        serverMetaData,
        Tools(capabilities.filterIsInstance<ToolCapability>()),
        Resources(capabilities.filterIsInstance<ResourceCapability>()),
        Prompts(capabilities.filterIsInstance<PromptCapability>()),
        Completions(capabilities.filterIsInstance<CompletionCapability>()),
    )

    private val clients = ConcurrentHashMap<SessionId, McpClient>()

    fun receive(sId: SessionId, request: Request, session: McpConnection<RSP, *>): RSP {
        val payload = McpJson.fields(McpJson.parse(request.bodyString())).toMap()

        return when {
            payload["method"] != null -> {
                val jsonReq = JsonRpcRequest(McpJson, payload)

                when (McpRpcMethod.of(jsonReq.method)) {
                    McpInitialize.Method ->
                        session.send(jsonReq.respondTo<McpInitialize.Request> { handleInitialize(it, sId, session) }, sId)

                    McpCompletion.Method ->
                        session.send(
                            jsonReq.respondTo<McpCompletion.Request> { completions.complete(it, request) },
                            sId
                        )

                    McpPing.Method -> session.send(
                        jsonReq.respondTo<McpPing.Request> { ServerMessage.Response.Empty },
                        sId
                    )

                    McpPrompt.Get.Method ->
                        session.send(jsonReq.respondTo<McpPrompt.Get.Request> { prompts.get(it, request) }, sId)

                    McpPrompt.List.Method ->
                        session.send(jsonReq.respondTo<McpPrompt.List.Request> { prompts.list(it, request) }, sId)

                    McpResource.Template.List.Method ->
                        session.send(jsonReq.respondTo<McpResource.Template.List.Request> {
                            resources.listTemplates(it, request)
                        }, sId)

                    McpResource.List.Method ->
                        session.send(jsonReq.respondTo<McpResource.List.Request> {
                            resources.listResources(
                                it,
                                request
                            )
                        }, sId)

                    McpResource.Read.Method ->
                        session.send(jsonReq.respondTo<McpResource.Read.Request> { resources.read(it, request) }, sId)

                    McpResource.Subscribe.Method -> {
                        val subscribeRequest = jsonReq.fromJsonRpc<McpResource.Subscribe.Request>()
                        resources.subscribe(sId, subscribeRequest) {
                            session.send(
                                McpResource.Updated.Notification(subscribeRequest.uri).toJsonRpc(McpResource.Updated),
                                sId
                            )
                        }
                        session.ok()
                    }

                    McpLogging.SetLevel.Method -> {
                        logger.setLevel(sId, jsonReq.fromJsonRpc<McpLogging.SetLevel.Request>().level)
                        session.ok()
                    }

                    McpResource.Unsubscribe.Method -> {
                        resources.unsubscribe(sId, jsonReq.fromJsonRpc())
                        session.ok()
                    }

                    McpInitialize.Initialized.Method -> session.ok()

                    Cancelled.Method -> session.ok()

                    McpProgress.Method -> session.ok()

                    McpRoot.Changed.Method -> {
                        val requestId = RequestId.random(random)
                        clients[sId]?.addCallback(requestId) { roots.update(it.fromJsonRpc()) }
                        session.send(
                            McpRoot.List.Request().toJsonRpc(McpRoot.List, McpJson.asJsonObject(requestId)),
                            sId
                        )
                    }

                    McpTool.Call.Method -> session.send(
                        jsonReq.respondTo<McpTool.Call.Request> { tools.call(it, request) },
                        sId
                    )

                    McpTool.List.Method -> session.send(
                        jsonReq.respondTo<McpTool.List.Request> { tools.list(it, request) },
                        sId
                    )

                    else -> session.send(ErrorMessage.MethodNotFound.toJsonRpc(jsonReq.id), sId)
                }
            }

            else -> {
                val jsonResult = JsonRpcResult(McpJson, payload)

                when {
                    jsonResult.isError() -> session.ok()
                    else -> with(McpJson) {
                        val id = jsonResult.id?.let { RequestId.parse(compact(it)) }
                        when (id) {
                            null -> session.ok()
                            else -> clients[sId]?.processResult(id, jsonResult)?.let { session.ok() }
                                ?: session.error()
                        }
                    }
                }
            }
        }
    }

    fun handleInitialize(
        request: McpInitialize.Request,
        sId: SessionId,
        connection: McpConnection<RSP, *>
    ): McpInitialize.Response {

        clients[sId] = McpClient()
        logger.subscribe(sId, LogLevel.error) { level, logger, data ->
            connection.send(
                McpLogging.LoggingMessage.Notification(level, logger, data).toJsonRpc(McpLogging.LoggingMessage),
                sId
            )
        }
        prompts.onChange(sId) {
            connection.send(McpPrompt.List.Changed.Notification.toJsonRpc(McpPrompt.List.Changed), sId)
        }
        resources.onChange(sId) {
            connection.send(
                McpResource.List.Changed.Notification.toJsonRpc(McpResource.List.Changed),
                sId
            )
        }
        tools.onChange(sId) { connection.send(McpTool.List.Changed.Notification.toJsonRpc(McpTool.List.Changed), sId) }

        sampling.onSampleClient(sId, request.clientInfo.name) { req, id ->
            clients[sId]?.addCallback(id) { sampling.receive(id, it.fromJsonRpc()) }
            connection.send(req.toJsonRpc(McpSampling, McpJson.asJsonObject(id)), sId)
        }

        connection.onClose(sId) {
            clients.remove(sId)
            prompts.remove(sId)
            resources.remove(sId)
            tools.remove(sId)
            sampling.remove(sId)
            logger.unsubscribe(sId)
        }

        return McpInitialize.Response(metaData.entity, metaData.capabilities, metaData.protocolVersion)
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
