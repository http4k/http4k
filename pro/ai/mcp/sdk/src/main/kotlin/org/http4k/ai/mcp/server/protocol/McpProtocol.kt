package org.http4k.ai.mcp.server.protocol

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.get
import org.http4k.ai.mcp.model.LogLevel.error
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.messages.McpCancelled
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.protocol.messages.McpInitialize
import org.http4k.ai.mcp.protocol.messages.McpLogging
import org.http4k.ai.mcp.protocol.messages.McpPing
import org.http4k.ai.mcp.protocol.messages.McpProgress
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpRoot
import org.http4k.ai.mcp.protocol.messages.McpTask
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.protocol.messages.ServerMessage
import org.http4k.ai.mcp.protocol.messages.fromJsonRpc
import org.http4k.ai.mcp.protocol.messages.toJsonRpc
import org.http4k.ai.mcp.server.capability.CompletionCapability
import org.http4k.ai.mcp.server.capability.PromptCapability
import org.http4k.ai.mcp.server.capability.ResourceCapability
import org.http4k.ai.mcp.server.capability.ServerCancellations
import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.ai.mcp.server.capability.ServerCompletions
import org.http4k.ai.mcp.server.capability.ServerPrompts
import org.http4k.ai.mcp.server.capability.ServerResources
import org.http4k.ai.mcp.server.capability.ServerRoots
import org.http4k.ai.mcp.server.capability.ServerTasks
import org.http4k.ai.mcp.server.capability.ServerTools
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.ai.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.asJsonObject
import org.http4k.ai.mcp.util.McpJson.nullNode
import org.http4k.ai.mcp.util.McpJson.parse
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.core.Request
import org.http4k.format.MoshiArray
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Models the MCP protocol in terms of message handling and session management.
 */
class McpProtocol<Transport>(
    internal val metaData: ServerMetaData,
    private val sessions: Sessions<Transport>,
    private val tools: Tools = ServerTools(),
    private val resources: Resources = ServerResources(),
    private val prompts: Prompts = ServerPrompts(),
    private val completions: Completions = ServerCompletions(),
    private val logger: Logger = ServerLogger(),
    private val roots: Roots = ServerRoots(),
    private val cancellations: Cancellations = ServerCancellations(),
    private val tasks: Tasks = ServerTasks(),
    private val mcpFilter: McpFilter = McpFilter.NoOp,
    private val onError: (Throwable) -> Unit = { it.printStackTrace(System.err) },
    private val random: Random = Random,
) {
    constructor(
        metaData: ServerMetaData,
        sessions: Sessions<Transport>,
        mcpFilter: McpFilter = McpFilter.NoOp,
        vararg capabilities: ServerCapability,
    ) : this(
        metaData,
        sessions,
        ServerTools(capabilities.flatMap { it }.filterIsInstance<ToolCapability>()),
        ServerResources(capabilities.flatMap { it }.filterIsInstance<ResourceCapability>()),
        ServerPrompts(capabilities.flatMap { it }.filterIsInstance<PromptCapability>()),
        ServerCompletions(capabilities.flatMap { it }.filterIsInstance<CompletionCapability>()),
        mcpFilter = mcpFilter,
    )

    private val clientTracking = ConcurrentHashMap<Session, ClientTracking>()

    fun receive(transport: Transport, session: Session, httpReq: Request): Result4k<McpNodeType, McpNodeType> {
        val rawPayload = runCatching { parse(httpReq.bodyString()) }.getOrElse { return error() }

        return when (rawPayload) {
            is MoshiArray -> Success(
                MoshiArray(
                    rawPayload.elements
                        .filterIsInstance<MoshiObject>()
                        .map { processMessage(transport, it, session, httpReq) }
                )
            )

            is MoshiObject -> Success(processMessage(transport, rawPayload, session, httpReq))
            else -> error()
        }
    }

    private fun ok() = Success(nullNode())
    private fun error() = Failure(nullNode())

    private fun processMessage(
        transport: Transport,
        rawPayload: McpNodeType,
        session: Session,
        httpReq: Request
    ): McpNodeType {
        val payload = McpJson.fields(rawPayload).toMap()

        val hf =
            McpHandlerFactory(transport, sessions, tasks, logger, random, clientTracking, onError, mcpFilter)

        return when {
            payload["method"] != null -> {
                val jsonReq = JsonRpcRequest(McpJson, payload)

                when (McpRpcMethod.of(jsonReq.method)) {
                    McpInitialize.Method ->
                        hf.responder<McpInitialize.Request>(session, jsonReq, httpReq) { it, _ ->
                            assign(Subscription(session), transport, httpReq)
                            handleInitialize(it, session)
                        }

                    McpCompletion.Method ->
                        hf.responder<McpCompletion.Request>(session, jsonReq, httpReq) { it, c ->
                            completions.complete(it, c, httpReq)
                        }

                    McpPing.Method ->
                        hf.responder<McpPing.Request>(session, jsonReq, httpReq) { _, _ ->
                            ServerMessage.Response.Empty
                        }

                    McpPrompt.Get.Method ->
                        hf.responder<McpPrompt.Get.Request>(session, jsonReq, httpReq) { it, c ->
                            prompts.get(it, c, httpReq)
                        }

                    McpPrompt.List.Method ->
                        hf.responder<McpPrompt.List.Request>(session, jsonReq, httpReq) { it, c ->
                            prompts.list(it, c, httpReq)
                        }

                    McpResource.ListTemplates.Method ->
                        hf.responder<McpResource.ListTemplates.Request>(
                            session,
                            jsonReq,
                            httpReq
                        ) { it, c ->
                            resources.listTemplates(it, c, httpReq)
                        }

                    McpResource.List.Method ->
                        hf.responder<McpResource.List.Request>(session, jsonReq, httpReq) { it, c ->
                            resources.listResources(it, c, httpReq)
                        }

                    McpResource.Read.Method -> {
                        hf.responder<McpResource.Read.Request>(session, jsonReq, httpReq) { it, c ->
                            resources.read(it, c, httpReq)
                        }
                    }

                    McpResource.Subscribe.Method -> {
                        hf.responder<McpResource.Subscribe.Request>(session, jsonReq, httpReq) { _, _ ->
                            when (resources) {
                                is ObservableResources -> {
                                    val subscribeRequest = jsonReq.fromJsonRpc(McpResource.Subscribe.Request::class)
                                    resources.subscribe(session, subscribeRequest) {
                                        sessions.request(
                                            Subscription(session),
                                            McpResource.Updated.Notification(subscribeRequest.uri)
                                                .toJsonRpc(McpResource.Updated)
                                        )
                                    }
                                }
                            }
                            ServerMessage.Response.Empty
                        }
                    }

                    McpLogging.SetLevel.Method ->
                        hf.responder<McpLogging.SetLevel.Request>(session, jsonReq, httpReq) { _, _ ->
                            logger.setLevel(session, jsonReq.fromJsonRpc(McpLogging.SetLevel.Request::class).level)
                            ServerMessage.Response.Empty
                        }

                    McpResource.Unsubscribe.Method ->
                        hf.responder<McpResource.Unsubscribe.Request>(session, jsonReq, httpReq) { _, _ ->
                            when (resources) {
                                is ObservableResources -> resources.unsubscribe(
                                    session,
                                    jsonReq.fromJsonRpc(McpResource.Unsubscribe.Request::class)
                                )
                            }
                            ServerMessage.Response.Empty
                        }

                    McpInitialize.Initialized.Method -> ok()

                    McpCancelled.Method -> {
                        cancellations.cancel(jsonReq.fromJsonRpc(McpCancelled.Notification::class))
                        ok()
                    }

                    McpProgress.Method -> ok()

                    McpRoot.Changed.Method -> {
                        clientTracking[session]?.let {
                            if (it.supportsRoots) {
                                val messageId = McpMessageId.random(random)
                                it.trackRequest(messageId) { roots.update(it.fromJsonRpc(McpRoot.List.Response::class)) }

                                sessions.respond(
                                    transport,
                                    session,
                                    McpRoot.List.Request().toJsonRpc(McpRoot.List, asJsonObject(messageId))
                                )
                            }
                        }
                        ok()
                    }

                    McpTool.Call.Method ->
                        hf.responder<McpTool.Call.Request>(session, jsonReq, httpReq) { it, c ->
                            tools.call(it, c, httpReq)
                        }

                    McpTool.List.Method ->
                        hf.responder<McpTool.List.Request>(session, jsonReq, httpReq) { it, c ->
                            tools.list(it, c, httpReq)
                        }

                    McpTask.Get.Method ->
                        hf.responder<McpTask.Get.Request>(session, jsonReq, httpReq) { it, c ->
                            tasks.get(session, it, c, httpReq)
                        }

                    McpTask.Result.Method ->
                        hf.responder<McpTask.Result.Request>(session, jsonReq, httpReq) { it, c ->
                            tasks.result(session, it, c, httpReq)
                        }

                    McpTask.Cancel.Method ->
                        hf.responder<McpTask.Cancel.Request>(session, jsonReq, httpReq) { it, c ->
                            tasks.cancel(session, it, c, httpReq)
                        }

                    McpTask.List.Method ->
                        hf.responder<McpTask.List.Request>(session, jsonReq, httpReq) { it, c ->
                            tasks.list(session, it, c, httpReq)
                        }

                    McpTask.Status.Method -> {
                        tasks.update(session, jsonReq.fromJsonRpc(McpTask.Status.Notification::class))
                        ok()
                    }

                    else -> sessions.respond(transport, session, MethodNotFound.toJsonRpc(jsonReq.id))
                }
            }

            else -> {
                val jsonResult = JsonRpcResult(McpJson, payload)
                when {
                    jsonResult.isError() -> ok()
                    else -> with(McpJson) {
                        val id = jsonResult.id?.let { McpMessageId.parse(compact(it)) }
                        when (id) {
                            null -> error()
                            else -> clientTracking[session]?.processResult(id, jsonResult)?.let { ok() }
                                ?: error()
                        }
                    }
                }
            }
        }.get()
    }

    fun handleInitialize(request: McpInitialize.Request, session: Session): McpInitialize.Response {
        clientTracking[session] = ClientTracking(request)

        val context = Subscription(session)

        logger.subscribe(session, error) { data, level, logger ->
            sessions.request(
                context,
                McpLogging.LoggingMessage.Notification(data, level, logger).toJsonRpc(McpLogging.LoggingMessage)
            )
        }

        if (prompts is ObservableCapability) {
            prompts.onChange(session) {
                sessions.request(
                    context,
                    McpPrompt.List.Changed.Notification.toJsonRpc(McpPrompt.List.Changed)
                )
            }
        }

        if (resources is ObservableCapability) {
            resources.onChange(session) {
                sessions.request(
                    context,
                    McpResource.List.Changed.Notification.toJsonRpc(McpResource.List.Changed)
                )
            }
        }

        if (tools is ObservableCapability) {
            tools.onChange(session) {
                sessions.request(
                    context,
                    McpTool.List.Changed.Notification.toJsonRpc(McpTool.List.Changed)
                )
            }
        }

        sessions.onClose(context) {
            if (prompts is ObservableCapability) prompts.remove(session)
            if (resources is ObservableCapability) resources.remove(session)
            if (tools is ObservableCapability) tools.remove(session)
            logger.unsubscribe(session)
            tasks.remove(session)
        }
        return McpInitialize.Response(
            metaData.entity, metaData.capabilities, when {
                metaData.protocolVersions.contains(request.protocolVersion) -> request.protocolVersion
                else -> metaData.protocolVersions.max()
            },
            metaData.instructions
        )
    }

    fun retrieveSession(req: Request) = sessions.retrieveSession(req)

    fun end(method: ClientRequestContext) {
        if (method is Subscription) clientTracking.remove(method.session)
        sessions.end(method)
    }

    fun assign(context: ClientRequestContext, transport: Transport, connectRequest: Request) =
        sessions.assign(context, transport, connectRequest)

    fun transportFor(context: ClientRequestContext) = sessions.transportFor(context)
}

