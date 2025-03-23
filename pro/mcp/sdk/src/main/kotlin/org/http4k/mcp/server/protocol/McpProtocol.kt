package org.http4k.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.format.MoshiNode
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.CompletionStatus.Finished
import org.http4k.mcp.model.LogLevel
import org.http4k.mcp.model.MessageId
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
import org.http4k.mcp.server.capability.ServerRequestProgress
import org.http4k.mcp.server.capability.ServerResources
import org.http4k.mcp.server.capability.ServerRoots
import org.http4k.mcp.server.capability.ServerSampling
import org.http4k.mcp.server.capability.ServerTools
import org.http4k.mcp.server.capability.ToolCapability
import org.http4k.mcp.server.sessions.Session
import org.http4k.mcp.server.sessions.Session.Invalid
import org.http4k.mcp.server.sessions.Session.Valid.Existing
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpJson.asJsonObject
import org.http4k.mcp.util.McpNodeType
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Models the MCP protocol in terms of message handling and session management.
 */
class McpProtocol<Transport, RSP : Any>(
    internal val metaData: ServerMetaData,
    private val sessions: Sessions<Transport, RSP>,
    private val tools: Tools = ServerTools(),
    private val resources: Resources = ServerResources(),
    private val prompts: Prompts = ServerPrompts(),
    private val completions: Completions = ServerCompletions(),
    private val sampling: Sampling = ServerSampling(Random),
    private val logger: Logger = ServerLogger(),
    private val roots: Roots = ServerRoots(),
    private val progress: RequestProgress = ServerRequestProgress(),
    private val random: Random = Random
) {
    constructor(
        serverMetaData: ServerMetaData,
        sessions: Sessions<Transport, RSP>,
        vararg capabilities: ServerCapability
    ) : this(
        serverMetaData,
        sessions,
        ServerTools(capabilities.flatMap { it }.filterIsInstance<ToolCapability>()),
        ServerResources(capabilities.flatMap { it }.filterIsInstance<ResourceCapability>()),
        ServerPrompts(capabilities.flatMap { it }.filterIsInstance<PromptCapability>()),
        ServerCompletions(capabilities.flatMap { it }.filterIsInstance<CompletionCapability>()),
    )

    private val clientRequests = ConcurrentHashMap<SessionId, ClientRequestTracking>()

    fun receive(
        transport: Transport,
        sId: SessionId,
        httpReq: Request
    ): RSP {
        val payload = runCatching {
            McpJson.fields(McpJson.parse(httpReq.bodyString())).toMap()
        }.getOrElse { return sessions.error() }

        return when {
            payload["method"] != null -> {
                val jsonReq = JsonRpcRequest(McpJson, payload)

                when (McpRpcMethod.of(jsonReq.method)) {
                    McpInitialize.Method ->
                        sessions.respond(
                            transport,
                            sId,
                            jsonReq.respondTo<McpInitialize.Request> { handleInitialize(it, sId) })

                    McpCompletion.Method ->
                        sessions.respond(
                            transport,
                            sId,
                            jsonReq.respondTo<McpCompletion.Request> { completions.complete(it, httpReq) }
                        )

                    McpPing.Method -> sessions.respond(
                        transport,
                        sId,
                        jsonReq.respondTo<McpPing.Request> { ServerMessage.Response.Empty }
                    )

                    McpPrompt.Get.Method ->
                        sessions.respond(
                            transport,
                            sId,
                            jsonReq.respondTo<McpPrompt.Get.Request> { prompts.get(it, httpReq) })

                    McpPrompt.List.Method ->
                        sessions.respond(
                            transport,
                            sId,
                            jsonReq.respondTo<McpPrompt.List.Request> { prompts.list(it, httpReq) })

                    McpResource.Template.List.Method ->
                        sessions.respond(transport, sId, jsonReq.respondTo<McpResource.Template.List.Request> {
                            resources.listTemplates(it, httpReq)
                        })

                    McpResource.List.Method ->
                        sessions.respond(transport, sId, jsonReq.respondTo<McpResource.List.Request> {
                            resources.listResources(
                                it,
                                httpReq
                            )
                        })

                    McpResource.Read.Method ->
                        sessions.respond(
                            transport,
                            sId,
                            jsonReq.respondTo<McpResource.Read.Request> { resources.read(it, httpReq) })

                    McpResource.Subscribe.Method -> {
                        val subscribeRequest = jsonReq.fromJsonRpc<McpResource.Subscribe.Request>()
                        resources.subscribe(sId, subscribeRequest) {
                            sessions.respond(
                                transport,
                                sId,
                                McpResource.Updated.Notification(subscribeRequest.uri).toJsonRpc(McpResource.Updated)
                            )
                        }
                        sessions.ok()
                    }

                    McpLogging.SetLevel.Method -> {
                        logger.setLevel(sId, jsonReq.fromJsonRpc<McpLogging.SetLevel.Request>().level)
                        sessions.ok()
                    }

                    McpResource.Unsubscribe.Method -> {
                        resources.unsubscribe(sId, jsonReq.fromJsonRpc())
                        sessions.ok()
                    }

                    McpInitialize.Initialized.Method -> sessions.ok()

                    Cancelled.Method -> sessions.ok()

                    McpProgress.Method -> sessions.ok()

                    McpRoot.Changed.Method -> {
                        val messageId = MessageId.random(random)
                        clientRequests[sId]?.trackRequest(messageId) { roots.update(it.fromJsonRpc()) }
                        sessions.respond(
                            transport,
                            sId,
                            McpRoot.List.Request().toJsonRpc(McpRoot.List, asJsonObject(messageId))
                        )
                        sessions.ok()
                    }

                    McpTool.Call.Method -> sessions.respond(
                        transport,
                        sId,
                        jsonReq.respondTo<McpTool.Call.Request> { tools.call(it, httpReq) }
                    )

                    McpTool.List.Method -> sessions.respond(
                        transport,
                        sId,
                        jsonReq.respondTo<McpTool.List.Request> { tools.list(it, httpReq) }
                    )

                    else -> sessions.respond(transport, sId, MethodNotFound.toJsonRpc(jsonReq.id))
                }
            }

            else -> {
                val jsonResult = JsonRpcResult(McpJson, payload)
                when {
                    jsonResult.isError() -> sessions.ok()
                    else -> with(McpJson) {
                        val id = jsonResult.id?.let { MessageId.parse(compact(it)) }
                        when (id) {
                            null -> sessions.error()
                            else -> clientRequests[sId]?.processResult(id, jsonResult)?.let { sessions.ok() }
                                ?: sessions.error()
                        }
                    }
                }
            }
        }
    }

    fun handleInitialize(request: McpInitialize.Request, sId: SessionId): McpInitialize.Response {
        if (!clientRequests.contains(sId)) clientRequests[sId] = ClientRequestTracking()

        logger.subscribe(sId, LogLevel.error) { level, logger, data ->
            sessions.request(
                sId,
                McpLogging.LoggingMessage.Notification(level, logger, data).toJsonRpc(McpLogging.LoggingMessage)
            )
        }
        prompts.onChange(sId) {
            sessions.request(sId, McpPrompt.List.Changed.Notification.toJsonRpc(McpPrompt.List.Changed))
        }
        progress.onProgress(sId) {
            sessions.request(sId, it.toJsonRpc(McpProgress))
        }
        resources.onChange(sId) {
            sessions.request(
                sId,
                McpResource.List.Changed.Notification.toJsonRpc(McpResource.List.Changed)
            )
        }
        tools.onChange(sId) {
            sessions.request(
                sId,
                McpTool.List.Changed.Notification.toJsonRpc(McpTool.List.Changed)
            )
        }

        sampling.onSampleClient(sId, request.clientInfo.name) { req, id ->
            clientRequests[sId]?.trackRequest(id) {
                sampling.receive(id, it.fromJsonRpc())
            }
            sessions.request(sId, req.toJsonRpc(McpSampling, asJsonObject(id)))
        }

        sessions.onClose(sId) {
            prompts.remove(sId)
            progress.remove(sId)
            resources.remove(sId)
            tools.remove(sId)
            sampling.remove(sId)
            logger.unsubscribe(sId)
        }

        return McpInitialize.Response(metaData.entity, metaData.capabilities, sId, metaData.protocolVersion)
    }

    fun validate(req: Request) = sessions.validate(req)

    fun end(session: Session) =
        when (session) {
            is Session.Valid -> {
                clientRequests.remove(session.sessionId)
                sessions.end(session.sessionId)
            }

            Invalid -> sessions.error()
        }

    fun assign(session: Session, transport: Transport, connectRequest: Request) = sessions.assign(session, transport, connectRequest)

    fun transportFor(session: Existing) = sessions.transportFor(session)

    private class ClientRequestTracking {
        private val calls = ConcurrentHashMap<MessageId, (JsonRpcResult<McpNodeType>) -> CompletionStatus>()

        fun trackRequest(id: MessageId, callback: (JsonRpcResult<McpNodeType>) -> CompletionStatus) {
            calls[id] = callback
        }

        fun processResult(id: MessageId, result: JsonRpcResult<MoshiNode>) {
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
