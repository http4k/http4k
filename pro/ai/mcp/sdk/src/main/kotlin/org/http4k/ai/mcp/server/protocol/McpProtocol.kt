/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.InitializeHandler
import org.http4k.ai.mcp.model.LogLevel.error
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.messages.ClientMessage
import org.http4k.ai.mcp.protocol.messages.McpCancelled
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.protocol.messages.McpInitialize
import org.http4k.ai.mcp.protocol.messages.McpLogging
import org.http4k.ai.mcp.protocol.messages.McpPing
import org.http4k.ai.mcp.protocol.messages.McpProgress
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpResource.Updated.Notification
import org.http4k.ai.mcp.protocol.messages.McpRoot
import org.http4k.ai.mcp.protocol.messages.McpTask
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.protocol.messages.ServerMessage
import org.http4k.ai.mcp.protocol.messages.fromJsonRpc
import org.http4k.ai.mcp.protocol.messages.toJsonRpc
import org.http4k.ai.mcp.server.capability.CompletionCapability
import org.http4k.ai.mcp.server.capability.PromptCapability
import org.http4k.ai.mcp.server.capability.ResourceCapability
import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.ai.mcp.server.capability.SimpleInitializeHandler
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.ai.mcp.server.capability.cancellations
import org.http4k.ai.mcp.server.capability.completions
import org.http4k.ai.mcp.server.capability.initializer
import org.http4k.ai.mcp.server.capability.logger
import org.http4k.ai.mcp.server.capability.prompts
import org.http4k.ai.mcp.server.capability.resources
import org.http4k.ai.mcp.server.capability.roots
import org.http4k.ai.mcp.server.capability.tasks
import org.http4k.ai.mcp.server.capability.tools
import org.http4k.ai.mcp.server.protocol.ClientRequestContext.ClientCall
import org.http4k.ai.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.ai.mcp.server.protocol.McpResponse.Accepted
import org.http4k.ai.mcp.server.protocol.McpResponse.Ok
import org.http4k.ai.mcp.server.protocol.McpResponse.Unknown
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.asJsonObject
import org.http4k.ai.mcp.util.McpJson.parse
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.core.Request
import org.http4k.format.MoshiNode
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Models the MCP protocol in terms of message handling and session management.
 */
class McpProtocol<Transport>(
    private val sessions: Sessions<Transport>,
    private val initializer: Initializer,
    private val tools: Tools = tools(),
    private val resources: Resources = resources(),
    private val prompts: Prompts = prompts(),
    private val completions: Completions = completions(),
    private val logger: Logger = logger(),
    private val roots: Roots = roots(),
    private val cancellations: Cancellations = cancellations(),
    private val tasks: Tasks = tasks(),
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
        sessions,
        initializer(SimpleInitializeHandler(metaData)),
        tools(capabilities.flatMap { it }.filterIsInstance<ToolCapability>()),
        resources(capabilities.flatMap { it }.filterIsInstance<ResourceCapability>()),
        prompts(capabilities.flatMap { it }.filterIsInstance<PromptCapability>()),
        completions(capabilities.flatMap { it }.filterIsInstance<CompletionCapability>()),
        mcpFilter = mcpFilter,
    )
    constructor(
        initializeHandler: InitializeHandler,
        sessions: Sessions<Transport>,
        mcpFilter: McpFilter = McpFilter.NoOp,
        vararg capabilities: ServerCapability,
    ) : this(
        sessions,
        initializer(initializeHandler),
        tools(capabilities.flatMap { it }.filterIsInstance<ToolCapability>()),
        resources(capabilities.flatMap { it }.filterIsInstance<ResourceCapability>()),
        prompts(capabilities.flatMap { it }.filterIsInstance<PromptCapability>()),
        completions(capabilities.flatMap { it }.filterIsInstance<CompletionCapability>()),
        mcpFilter = mcpFilter,
    )

    private val clientTracking = ConcurrentHashMap<Session, ClientTracking>()

    fun receive(transport: Transport, sessionState: ValidSessionState, httpReq: Request): McpResponse {
        val rawPayload = runCatching { parse(httpReq.bodyString()) }
            .getOrElse { return Ok(ErrorMessage.ParseError.toJsonRpc(null)) }

        val mcpRequest = rawPayload.toMcpRequest(sessionState, httpReq)

        return responseFor(mcpRequest, sessionState, transport)
    }

    private fun responseFor(
        mcpRequest: McpRequest,
        sessionState: ValidSessionState,
        transport: Transport
    ): McpResponse {

        val filter = mcpFilter
            .then(AssignAndCloseSession(sessions, transport))

        return when (mcpRequest.json) {
            is JsonRpcRequest<McpNodeType> -> {
                val method = McpRpcMethod.of(mcpRequest.json.method)
                when {
                    sessionState is NewSession || method == McpInitialize.Method ->
                        respond<McpInitialize.Request>(filter, mcpRequest) {
                            handleInitialize(it, mcpRequest.http, sessionState.session)
                        }

                    method == McpCompletion.Method ->
                        respond<McpCompletion.Request>(filter, mcpRequest) {
                            completions.complete(it, clientFor(sessionState.session), mcpRequest.http)
                        }

                    method == McpPing.Method ->
                        respond<McpPing.Request>(filter, mcpRequest) {
                            ServerMessage.Response.Empty
                        }


                    method == McpPrompt.Get.Method ->
                        respond<McpPrompt.Get.Request>(filter, mcpRequest) {
                            prompts.get(it, clientFor(sessionState.session), mcpRequest.http)
                        }


                    method == McpPrompt.List.Method ->
                        respond<McpPrompt.List.Request>(filter, mcpRequest) {
                            prompts.list(it, clientFor(sessionState.session), mcpRequest.http)
                        }


                    method == McpResource.ListTemplates.Method ->
                        respond<McpResource.ListTemplates.Request>(filter, mcpRequest) {
                            resources.listTemplates(it, clientFor(sessionState.session), mcpRequest.http)
                        }


                    method == McpResource.List.Method ->
                        respond<McpResource.List.Request>(filter, mcpRequest) {
                            resources.listResources(it, clientFor(sessionState.session), mcpRequest.http)
                        }


                    method == McpResource.Read.Method -> {
                        respond<McpResource.Read.Request>(filter, mcpRequest) {
                            resources.read(it, clientFor(sessionState.session), mcpRequest.http)
                        }
                    }

                    method == McpResource.Subscribe.Method -> {
                        respond<McpResource.Subscribe.Request>(filter, mcpRequest) {
                            when (resources) {
                                is ObservableResources -> {
                                    val subscribeRequest =
                                        mcpRequest.json.fromJsonRpc(McpResource.Subscribe.Request::class)
                                    resources.subscribe(sessionState.session, subscribeRequest) {
                                        sessions.request(
                                            Subscription(sessionState.session),
                                            Notification(subscribeRequest.uri)
                                                .toJsonRpc(McpResource.Updated)
                                        )
                                    }
                                }
                            }
                            ServerMessage.Response.Empty
                        }
                    }

                    method == McpLogging.SetLevel.Method ->
                        respond<McpLogging.SetLevel.Request>(filter, mcpRequest) {
                            logger.setLevel(
                                sessionState.session,
                                mcpRequest.json.fromJsonRpc(McpLogging.SetLevel.Request::class).level
                            )
                            ServerMessage.Response.Empty
                        }


                    method == McpResource.Unsubscribe.Method ->
                        respond<McpResource.Unsubscribe.Request>(filter, mcpRequest) {
                            when (resources) {
                                is ObservableResources -> resources.unsubscribe(
                                    sessionState.session,
                                    mcpRequest.json.fromJsonRpc(McpResource.Unsubscribe.Request::class)
                                )
                            }
                            ServerMessage.Response.Empty
                        }


                    method == McpInitialize.Initialized.Method -> Accepted

                    method == McpCancelled.Method -> {
                        cancellations.cancel(mcpRequest.json.fromJsonRpc(McpCancelled.Notification::class))
                        Accepted
                    }

                    method == McpProgress.Method -> Accepted

                    method == McpRoot.Changed.Method -> {
                        clientTracking[sessionState.session]?.let {
                            if (it.supportsRoots) {
                                val messageId = McpMessageId.random(random)
                                it.trackRequest(messageId) { roots.update(it.fromJsonRpc(McpRoot.List.Response::class)) }

                                sessions.respond(
                                    transport,
                                    ClientCall(sessionState.session),
                                    McpRoot.List.Request().toJsonRpc(McpRoot.List, asJsonObject(messageId))
                                )
                            }
                        }
                        Accepted
                    }

                    method == McpTool.Call.Method ->
                        respond<McpTool.Call.Request>(filter, mcpRequest) {
                            tools.call(it, clientFor(sessionState.session), mcpRequest.http)
                        }


                    method == McpTool.List.Method ->
                        respond<McpTool.List.Request>(filter, mcpRequest) {
                            tools.list(it, clientFor(sessionState.session), mcpRequest.http)
                        }


                    method == McpTask.Get.Method ->
                        respond<McpTask.Get.Request>(filter, mcpRequest) {
                            tasks.get(sessionState.session, it, clientFor(sessionState.session), mcpRequest.http)
                        }


                    method == McpTask.Result.Method ->
                        respond<McpTask.Result.Request>(filter, mcpRequest) {
                            tasks.result(sessionState.session, it, clientFor(sessionState.session), mcpRequest.http)
                        }


                    method == McpTask.Cancel.Method ->
                        respond<McpTask.Cancel.Request>(filter, mcpRequest) {
                            tasks.cancel(sessionState.session, it, clientFor(sessionState.session), mcpRequest.http)
                        }


                    method == McpTask.List.Method ->
                        respond<McpTask.List.Request>(filter, mcpRequest) {
                            tasks.list(sessionState.session, it, clientFor(sessionState.session), mcpRequest.http)
                        }


                    method == McpTask.Status.Method -> {
                        tasks.update(
                            sessionState.session,
                            mcpRequest.json.fromJsonRpc(McpTask.Status.Notification::class)
                        )
                        Accepted
                    }

                    else -> Ok(
                        sessions.respond(
                            transport,
                            ClientCall(sessionState.session),
                            MethodNotFound.toJsonRpc(mcpRequest.json.id)
                        )
                    )
                }
            }

            is JsonRpcResult<McpNodeType> -> {
                when {
                    mcpRequest.json.isError() -> Accepted
                    else -> with(McpJson) {
                        val id = mcpRequest.json.id?.let { McpMessageId.parse(compact(it)) }
                        when (id) {
                            null -> Ok(ErrorMessage.ParseError.toJsonRpc(null))
                            else -> clientTracking[sessionState.session]?.processResult(id, mcpRequest.json)
                                ?.let { Accepted }
                                ?: Unknown
                        }
                    }
                }
            }
        }
    }

    private fun clientFor(session: Session): SessionBasedClient = SessionBasedClient(
        { sessions.request(ClientCall(session), it) },
        session,
        logger,
        tasks,
        random,
        { clientTracking[session] ?: throw McpException(ErrorMessage.InternalError) }
    )

    private fun handleInitialize(
        request: McpInitialize.Request,
        http: Request,
        session: Session
    ): McpInitialize.Response = initializer(request, http)
        .also { clientTracking[session] = ClientTracking(request) }

    fun retrieveSession(req: Request) = sessions.retrieveSession(req)

    fun unsubscribe(context: Subscription) {
        clientTracking.remove(context.session)
        sessions.end(context)
    }

    fun subscribe(context: Subscription, transport: Transport, connectRequest: Request) {
        sessions.assign(context, transport, connectRequest)

        logger.subscribe(context.session, error) { data, level, logger ->
            sessions.request(
                context,
                McpLogging.LoggingMessage.Notification(data, level, logger).toJsonRpc(McpLogging.LoggingMessage)
            )
        }

        prompts.onChange(context.session) {
            sessions.request(
                context,
                McpPrompt.List.Changed.Notification().toJsonRpc(McpPrompt.List.Changed)
            )
        }

        resources.onChange(context.session) {
            sessions.request(
                context,
                McpResource.List.Changed.Notification().toJsonRpc(McpResource.List.Changed)
            )
        }

        tools.onChange(context.session) {
            sessions.request(
                context,
                McpTool.List.Changed.Notification().toJsonRpc(McpTool.List.Changed)
            )
        }

        sessions.onClose(context) {
            prompts.remove(context.session)
            resources.remove(context.session)
            tools.remove(context.session)
            logger.unsubscribe(context.session)
            tasks.remove(context.session)
        }
    }

    fun transportFor(context: ClientRequestContext) = sessions.transportFor(context)

    private inline fun <reified IN : ClientMessage.Request> respond(
        filter: McpFilter,
        mcpRequest: McpRequest,
        noinline fn: (IN) -> ServerMessage.Response
    ): McpResponse = filter
        .then(AdaptingMcpHandlerFactory(onError)(IN::class, fn))(mcpRequest)
}

private fun MoshiNode.toMcpRequest(
    sessionState: ValidSessionState,
    httpReq: Request
): McpRequest {
    val payload = McpJson.fields(this).toMap()

    return McpRequest(
        sessionState.session,
        if (payload["method"] != null) JsonRpcRequest(McpJson, payload) else JsonRpcResult(McpJson, payload),
        httpReq
    )
}
