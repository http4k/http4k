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
import org.http4k.ai.mcp.protocol.messages.McpResource.Updated.Notification.Params as UpdatedNotificationParams

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
    onError: (Throwable) -> Unit = { it.printStackTrace(System.err) },
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
    private val handlerFactory = AdaptingMcpHandlerFactory(onError)

    private val handlers: Map<McpRpcMethod, McpHandler> = mapOf(
        McpPing.Method to adapted<McpPing.Request.Params> { _, _ ->
            ServerMessage.Response.Empty
        },
        McpCompletion.Method to adapted<McpCompletion.Request.Params> { req, mcp ->
            completions.complete(req, clientFor(mcp.session), mcp.http)
        },
        McpPrompt.Get.Method to adapted<McpPrompt.Get.Request.Params> { req, mcp ->
            prompts.get(req, clientFor(mcp.session), mcp.http)
        },
        McpPrompt.List.Method to adapted<McpPrompt.List.Request.Params> { req, mcp ->
            prompts.list(req, clientFor(mcp.session), mcp.http)
        },
        McpResource.ListTemplates.Method to adapted<McpResource.ListTemplates.Request.Params> { req, mcp ->
            resources.listTemplates(req, clientFor(mcp.session), mcp.http)
        },
        McpResource.List.Method to adapted<McpResource.List.Request.Params> { req, mcp ->
            resources.listResources(req, clientFor(mcp.session), mcp.http)
        },
        McpResource.Read.Method to adapted<McpResource.Read.Request.Params> { req, mcp ->
            resources.read(req, clientFor(mcp.session), mcp.http)
        },
        McpResource.Subscribe.Method to adapted<McpResource.Subscribe.Request.Params> { req, mcp ->
            when (resources) {
                is ObservableResources -> resources.subscribe(mcp.session, req) {
                    sessions.request(
                        Subscription(mcp.session),
                        UpdatedNotificationParams(req.uri).toJsonRpc(McpResource.Updated)
                    )
                }
            }
            ServerMessage.Response.Empty
        },
        McpResource.Unsubscribe.Method to adapted<McpResource.Unsubscribe.Request.Params> { req, mcp ->
            when (resources) {
                is ObservableResources -> resources.unsubscribe(mcp.session, req)
            }
            ServerMessage.Response.Empty
        },
        McpLogging.SetLevel.Method to adapted<McpLogging.SetLevel.Request.Params> { req, mcp ->
            logger.setLevel(mcp.session, req.level)
            ServerMessage.Response.Empty
        },
        McpTool.Call.Method to adapted<McpTool.Call.Request.Params> { req, mcp ->
            tools.call(req, clientFor(mcp.session), mcp.http)
        },
        McpTool.List.Method to adapted<McpTool.List.Request.Params> { req, mcp ->
            tools.list(req, clientFor(mcp.session), mcp.http)
        },
        McpTask.Get.Method to adapted<McpTask.Get.Request.Params> { req, mcp ->
            tasks.get(mcp.session, req, clientFor(mcp.session), mcp.http)
        },
        McpTask.Result.Method to adapted<McpTask.Result.Request.Params> { req, mcp ->
            tasks.result(mcp.session, req, clientFor(mcp.session), mcp.http)
        },
        McpTask.Cancel.Method to adapted<McpTask.Cancel.Request.Params> { req, mcp ->
            tasks.cancel(mcp.session, req, clientFor(mcp.session), mcp.http)
        },
        McpTask.List.Method to adapted<McpTask.List.Request.Params> { req, mcp ->
            tasks.list(mcp.session, req, clientFor(mcp.session), mcp.http)
        },
        McpInitialize.Initialized.Method to { _ -> Accepted },
        McpProgress.Method to { _ -> Accepted },
        McpCancelled.Method to { mcp ->
            cancellations.cancel(
                (mcp.json as JsonRpcRequest<McpNodeType>).fromJsonRpc(McpCancelled.Notification.Params::class)
            )
            Accepted
        },
        McpTask.Status.Method to { mcp ->
            tasks.update(
                mcp.session,
                (mcp.json as JsonRpcRequest<McpNodeType>).fromJsonRpc(McpTask.Status.Notification.Params::class)
            )
            Accepted
        },
    )

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
        val filter = mcpFilter.then(AssignAndCloseSession(sessions, transport))

        return when (mcpRequest.json) {
            is JsonRpcRequest<McpNodeType> -> {
                val method = McpRpcMethod.of(mcpRequest.json.method)
                when {
                    sessionState is NewSession || method == McpInitialize.Method ->
                        filter.then(adapted<McpInitialize.Request.Params> { req, mcp ->
                            handleInitialize(req, mcp.http, mcp.session)
                        })(mcpRequest)

                    method == McpRoot.Changed.Method -> handleRootChanged(mcpRequest, transport)

                    else -> handlers[method]?.let { filter.then(it)(mcpRequest) }
                        ?: Ok(
                            sessions.respond(
                                transport,
                                ClientCall(mcpRequest.session),
                                MethodNotFound.toJsonRpc(mcpRequest.json.id)
                            )
                        )
                }
            }

            is JsonRpcResult<McpNodeType> -> handleResult(mcpRequest.json, sessionState)
        }
    }

    private fun handleRootChanged(mcpRequest: McpRequest, transport: Transport): McpResponse {
        clientTracking[mcpRequest.session]?.let {
            if (it.supportsRoots) {
                val messageId = McpMessageId.random(random)
                it.trackRequest(messageId) { roots.update(it.fromJsonRpc(McpRoot.List.Response.Result::class)) }

                sessions.respond(
                    transport,
                    ClientCall(mcpRequest.session),
                    McpRoot.List.Request.Params().toJsonRpc(McpRoot.List, asJsonObject(messageId))
                )
            }
        }
        return Accepted
    }

    private fun handleResult(result: JsonRpcResult<McpNodeType>, sessionState: ValidSessionState) = when {
        result.isError() -> Accepted
        else -> with(McpJson) {
            val id = result.id?.let { McpMessageId.parse(compact(it)) }
            when (id) {
                null -> Ok(ErrorMessage.ParseError.toJsonRpc(null))
                else -> clientTracking[sessionState.session]?.processResult(id, result)
                    ?.let { Accepted }
                    ?: Unknown
            }
        }
    }

    private inline fun <reified IN : ClientMessage.Request> adapted(
        crossinline fn: (IN, McpRequest) -> ServerMessage.Response
    ): McpHandler = { mcpRequest ->
        handlerFactory(IN::class) { fn(it, mcpRequest) }(mcpRequest)
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
        request: McpInitialize.Request.Params,
        http: Request,
        session: Session
    ): McpInitialize.Response.Result = initializer(request, http)
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
                McpLogging.LoggingMessage.Notification.Params(data, level, logger).toJsonRpc(McpLogging.LoggingMessage)
            )
        }

        prompts.onChange(context.session) {
            sessions.request(
                context,
                McpPrompt.List.Changed.Notification.Params().toJsonRpc(McpPrompt.List.Changed)
            )
        }

        resources.onChange(context.session) {
            sessions.request(
                context,
                McpResource.List.Changed.Notification.Params().toJsonRpc(McpResource.List.Changed)
            )
        }

        tools.onChange(context.session) {
            sessions.request(
                context,
                McpTool.List.Changed.Notification.Params().toJsonRpc(McpTool.List.Changed)
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
