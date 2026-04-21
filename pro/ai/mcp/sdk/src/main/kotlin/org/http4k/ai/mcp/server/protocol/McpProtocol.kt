/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.get
import org.http4k.ai.mcp.Client
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
import org.http4k.ai.mcp.server.capability.cancellations
import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.ai.mcp.server.capability.completions
import org.http4k.ai.mcp.server.capability.initializer
import org.http4k.ai.mcp.server.capability.roots
import org.http4k.ai.mcp.server.capability.tasks
import org.http4k.ai.mcp.server.capability.SimpleInitializeHandler
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.ai.mcp.server.capability.logger
import org.http4k.ai.mcp.server.capability.prompts
import org.http4k.ai.mcp.server.capability.resources
import org.http4k.ai.mcp.server.capability.tools
import org.http4k.ai.mcp.server.protocol.ClientRequestContext.ClientCall
import org.http4k.ai.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.asJsonObject
import org.http4k.ai.mcp.util.McpJson.nullNode
import org.http4k.ai.mcp.util.McpJson.parse
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.core.Request
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

    fun receive(transport: Transport, session: Session, httpReq: Request): Result4k<McpNodeType, McpNodeType> {
        val rawPayload = runCatching { parse(httpReq.bodyString()) }.getOrElse { return Success(ErrorMessage.ParseError.toJsonRpc(null)) }

        val payload = McpJson.fields(rawPayload).toMap()

        val context = ClientCall(session)

        val response = when {
            payload["method"] != null -> {
                val jsonReq = JsonRpcRequest(McpJson, payload)

                val mcpRequest = McpRequest(session, jsonReq, httpReq)

                when (McpRpcMethod.of(jsonReq.method)) {
                    McpInitialize.Method ->
                        respond<McpInitialize.Request>(transport, mcpRequest, context) { it, _ ->
                            assign(Subscription(session), transport, httpReq)
                            handleInitialize(it, httpReq, session)
                        }


                    McpCompletion.Method ->
                        respond<McpCompletion.Request>(transport, mcpRequest, context) { it, c ->
                            completions.complete(it, c, httpReq)
                        }


                    McpPing.Method ->
                        respond<McpPing.Request>(transport, mcpRequest, context) { _, _ ->
                            ServerMessage.Response.Empty
                        }


                    McpPrompt.Get.Method ->
                        respond<McpPrompt.Get.Request>(transport, mcpRequest, context) { it, c ->
                            prompts.get(it, c, httpReq)
                        }


                    McpPrompt.List.Method ->
                        respond<McpPrompt.List.Request>(transport, mcpRequest, context) { it, c ->
                            prompts.list(it, c, httpReq)
                        }


                    McpResource.ListTemplates.Method ->
                        respond<McpResource.ListTemplates.Request>(
                            transport,
                            mcpRequest,
                            context
                        ) { it, c ->
                            resources.listTemplates(it, c, httpReq)
                        }


                    McpResource.List.Method ->
                        respond<McpResource.List.Request>(transport, mcpRequest, context) { it, c ->
                            resources.listResources(it, c, httpReq)
                        }


                    McpResource.Read.Method -> {
                        respond<McpResource.Read.Request>(transport, mcpRequest, context) { it, c ->
                            resources.read(it, c, httpReq)
                        }
                    }

                    McpResource.Subscribe.Method -> {
                        respond<McpResource.Subscribe.Request>(transport, mcpRequest, context) { _, _ ->
                            when (resources) {
                                is ObservableResources -> {
                                    val subscribeRequest = jsonReq.fromJsonRpc(McpResource.Subscribe.Request::class)
                                    resources.subscribe(session, subscribeRequest) {
                                        sessions.request(
                                            Subscription(session),
                                            Notification(subscribeRequest.uri)
                                                .toJsonRpc(McpResource.Updated)
                                        )
                                    }
                                }
                            }
                            ServerMessage.Response.Empty
                        }
                    }

                    McpLogging.SetLevel.Method ->
                        respond<McpLogging.SetLevel.Request>(transport, mcpRequest, context) { _, _ ->
                            logger.setLevel(session, jsonReq.fromJsonRpc(McpLogging.SetLevel.Request::class).level)
                            ServerMessage.Response.Empty
                        }


                    McpResource.Unsubscribe.Method ->
                        respond<McpResource.Unsubscribe.Request>(transport, mcpRequest, context) { _, _ ->
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
                                    context,
                                    McpRoot.List.Request().toJsonRpc(McpRoot.List, asJsonObject(messageId))
                                )
                            }
                        }
                        ok()
                    }

                    McpTool.Call.Method ->
                        respond<McpTool.Call.Request>(transport, mcpRequest, context) { it, c ->
                            tools.call(it, c, httpReq)
                        }


                    McpTool.List.Method ->
                        respond<McpTool.List.Request>(transport, mcpRequest, context) { it, c ->
                            tools.list(it, c, httpReq)
                        }


                    McpTask.Get.Method ->
                        respond<McpTask.Get.Request>(transport, mcpRequest, context) { it, c ->
                            tasks.get(session, it, c, httpReq)
                        }


                    McpTask.Result.Method ->
                        respond<McpTask.Result.Request>(transport, mcpRequest, context) { it, c ->
                            tasks.result(session, it, c, httpReq)
                        }


                    McpTask.Cancel.Method ->
                        respond<McpTask.Cancel.Request>(transport, mcpRequest, context) { it, c ->
                            tasks.cancel(session, it, c, httpReq)
                        }


                    McpTask.List.Method ->
                        respond<McpTask.List.Request>(transport, mcpRequest, context) { it, c ->
                            tasks.list(session, it, c, httpReq)
                        }


                    McpTask.Status.Method -> {
                        tasks.update(session, jsonReq.fromJsonRpc(McpTask.Status.Notification::class))
                        ok()
                    }

                    else -> sessions.respond(transport, context, MethodNotFound.toJsonRpc(jsonReq.id))
                }
            }

            else -> {
                val jsonResult = JsonRpcResult(McpJson, payload)
                when {
                    jsonResult.isError() -> ok()
                    else -> with(McpJson) {
                        val id = jsonResult.id?.let { McpMessageId.parse(compact(it)) }
                        when (id) {
                            null -> Success(ErrorMessage.ParseError.toJsonRpc(null))
                            else -> clientTracking[session]?.processResult(id, jsonResult)?.let { ok() }
                                ?: error()
                        }
                    }
                }
            }
        }.get()

        return Success(response)
    }

    private fun ok() = Success(nullNode())
    private fun error() = Failure(nullNode())

    private inline fun <reified IN : ClientMessage.Request> respond(
        transport: Transport,
        mcpRequest: McpRequest,
        callCtx: ClientCall,
        noinline fn: (IN, Client) -> ServerMessage.Response
    ): Result4k<McpNodeType, McpNodeType> {
        val client = clientFor(callCtx)

        val handler = mcpFilter
            .then(AssignAndCloseSession(sessions, transport))
            .then(AdaptingMcpHandler(onError)(IN::class, fn, client))

        return sessions.respond(transport, callCtx, handler(mcpRequest).json)
    }

    private fun clientFor(context: ClientRequestContext): SessionBasedClient = SessionBasedClient(
        { sessions.request(context, it) },
        context.session,
        logger,
        tasks,
        random,
        { clientTracking[context.session] ?: throw McpException(ErrorMessage.InternalError) }
    )

    fun handleInitialize(request: McpInitialize.Request, http: Request, session: Session): McpInitialize.Response {
        val response = initializer(request, http)

        clientTracking[session] = ClientTracking(request)

        val context = Subscription(session)

        logger.subscribe(session, error) { data, level, logger ->
            sessions.request(
                context,
                McpLogging.LoggingMessage.Notification(data, level, logger).toJsonRpc(McpLogging.LoggingMessage)
            )
        }

        prompts.onChange(session) {
            sessions.request(
                context,
                McpPrompt.List.Changed.Notification().toJsonRpc(McpPrompt.List.Changed)
            )
        }

        resources.onChange(session) {
            sessions.request(
                context,
                McpResource.List.Changed.Notification().toJsonRpc(McpResource.List.Changed)
            )
        }

        tools.onChange(session) {
            sessions.request(
                context,
                McpTool.List.Changed.Notification().toJsonRpc(McpTool.List.Changed)
            )
        }

        sessions.onClose(context) {
            prompts.remove(session)
            resources.remove(session)
            tools.remove(session)
            logger.unsubscribe(session)
            tasks.remove(session)
        }
        return response
    }

    fun retrieveSession(req: Request) = sessions.retrieveSession(req)

    fun end(context: ClientRequestContext) {
        if (context is Subscription) clientTracking.remove(context.session)
        sessions.end(context)
    }

    fun assign(context: ClientRequestContext, transport: Transport, connectRequest: Request) =
        sessions.assign(context, transport, connectRequest)

    fun transportFor(context: ClientRequestContext) = sessions.transportFor(context)
}

