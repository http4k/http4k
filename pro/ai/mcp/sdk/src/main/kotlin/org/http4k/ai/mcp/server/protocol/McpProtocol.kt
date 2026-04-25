/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.InitializeHandler
import org.http4k.ai.mcp.model.LogLevel.error
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.messages.McpCancelled
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.protocol.messages.McpElicitations
import org.http4k.ai.mcp.protocol.messages.McpInitialize
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcEmptyResponse
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.protocol.messages.McpLogging
import org.http4k.ai.mcp.protocol.messages.McpPing
import org.http4k.ai.mcp.protocol.messages.McpProgress
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpRoot
import org.http4k.ai.mcp.protocol.messages.McpSampling
import org.http4k.ai.mcp.protocol.messages.McpTask
import org.http4k.ai.mcp.protocol.messages.McpTool
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
import org.http4k.ai.mcp.util.McpJson.parse
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage
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
        val body = httpReq.bodyString()
        val rawPayload = runCatching { parse(body) }
            .getOrElse { return Ok(McpJsonRpcErrorResponse(null, ErrorMessage.ParseError)) }

        val payload = McpJson.fields(rawPayload).toMap()

        return if (payload["method"] != null) {
            val message = runCatching { McpJson.asA<McpJsonRpcRequest>(body) }
                .getOrElse { return Ok(McpJsonRpcErrorResponse(payload["id"], ErrorMessage.InvalidRequest)) }

            val mcpHandler = mcpFilter.then(AssignAndCloseSession(sessions, transport)).then(handler)

            mcpHandler(McpRequest(sessionState.session, message, httpReq))
        } else {
            handleResult(JsonRpcResult(McpJson, payload), sessionState)
        }
    }

    private val handler: McpHandler = { mcp ->
        runCatching {
            when (mcp.message) {
                is McpInitialize.Request -> Ok(
                    McpInitialize.Response(
                        handleInitialize(mcp.message.params, mcp.http, mcp.session), mcp.message.id
                    )
                )

                is McpPing.Request -> Ok(McpJsonRpcEmptyResponse(mcp.message.id))
                is McpCompletion.Request -> Ok(
                    McpCompletion.Response(
                        completions.complete(
                            mcp.message.params,
                            clientFor(mcp.session),
                            mcp.http
                        ), mcp.message.id
                    )
                )

                is McpPrompt.Get.Request -> Ok(
                    McpPrompt.Get.Response(
                        prompts.get(
                            mcp.message.params,
                            clientFor(mcp.session),
                            mcp.http
                        ), mcp.message.id
                    )
                )

                is McpPrompt.List.Request -> Ok(
                    McpPrompt.List.Response(
                        prompts.list(
                            mcp.message.params ?: McpPrompt.List.Request.Params(),
                            clientFor(mcp.session),
                            mcp.http
                        ), mcp.message.id
                    )
                )

                is McpResource.ListTemplates.Request -> Ok(
                    McpResource.ListTemplates.Response(
                        resources.listTemplates(
                            mcp.message.params ?: McpResource.ListTemplates.Request.Params(),
                            clientFor(mcp.session),
                            mcp.http
                        ), mcp.message.id
                    )
                )

                is McpResource.List.Request -> Ok(
                    McpResource.List.Response(
                        resources.listResources(
                            mcp.message.params ?: McpResource.List.Request.Params(),
                            clientFor(mcp.session),
                            mcp.http
                        ), mcp.message.id
                    )
                )

                is McpResource.Read.Request -> Ok(
                    McpResource.Read.Response(
                        resources.read(
                            mcp.message.params,
                            clientFor(mcp.session),
                            mcp.http
                        ), mcp.message.id
                    )
                )

                is McpResource.Subscribe.Request -> {
                    if (resources is ObservableResources) resources.subscribe(mcp.session, mcp.message.params) {
                        sessions.request(
                            Subscription(mcp.session),
                            McpResource.Updated.Notification(
                                McpResource.Updated.Notification.Params(mcp.message.params.uri)
                            )
                        )
                    }
                    Ok(McpJsonRpcEmptyResponse(mcp.message.id))
                }

                is McpResource.Unsubscribe.Request -> {
                    if (resources is ObservableResources) resources.unsubscribe(mcp.session, mcp.message.params)
                    Ok(McpJsonRpcEmptyResponse(mcp.message.id))
                }

                is McpLogging.SetLevel.Request -> {
                    logger.setLevel(mcp.session, mcp.message.params.level)
                    Ok(McpJsonRpcEmptyResponse(mcp.message.id))
                }

                is McpTool.Call.Request -> Ok(
                    McpTool.Call.Response(
                        tools.call(
                            mcp.message.params,
                            clientFor(mcp.session),
                            mcp.http
                        ), mcp.message.id
                    )
                )

                is McpTool.List.Request -> Ok(
                    McpTool.List.Response(
                        tools.list(
                            mcp.message.params ?: McpTool.List.Request.Params(),
                            clientFor(mcp.session),
                            mcp.http
                        ), mcp.message.id
                    )
                )

                is McpTask.Get.Request -> Ok(
                    McpTask.Get.Response(
                        tasks.get(
                            mcp.session,
                            mcp.message.params,
                            clientFor(mcp.session),
                            mcp.http
                        ), mcp.message.id
                    )
                )

                is McpTask.Result.Request -> Ok(
                    McpTask.Result.Response(
                        tasks.result(
                            mcp.session,
                            mcp.message.params,
                            clientFor(mcp.session),
                            mcp.http
                        ), mcp.message.id
                    )
                )

                is McpTask.Cancel.Request -> Ok(
                    McpTask.Cancel.Response(
                        tasks.cancel(
                            mcp.session,
                            mcp.message.params,
                            clientFor(mcp.session),
                            mcp.http
                        ), mcp.message.id
                    )
                )

                is McpTask.List.Request -> Ok(
                    McpTask.List.Response(
                        tasks.list(
                            mcp.session,
                            mcp.message.params,
                            clientFor(mcp.session),
                            mcp.http
                        ), mcp.message.id
                    )
                )

                is McpInitialize.Initialized.Notification -> Accepted
                is McpProgress.Notification -> Accepted
                is McpCancelled.Notification -> {
                    cancellations.cancel(mcp.message.params); Accepted
                }

                is McpTask.Status.Notification -> {
                    tasks.update(mcp.session, mcp.message.params); Accepted
                }

                is McpRoot.Changed.Notification -> {
                    handleRootChanged(mcp.session); Accepted
                }

                is McpPrompt.List.Changed.Notification -> Accepted
                is McpTool.List.Changed.Notification -> Accepted
                is McpResource.List.Changed.Notification -> Accepted
                is McpResource.Updated.Notification -> Accepted
                is McpLogging.LoggingMessage.Notification -> Accepted
                is McpElicitations.Complete.Notification -> Accepted
                is McpSampling.Request -> Accepted
                is McpElicitations.Request -> Accepted
                is McpRoot.List.Request -> Accepted
            }
        }.getOrElse { handleError(it, mcp.message.id) }
    }

    private fun handleError(e: Throwable, id: Any?): McpResponse = Ok(
        McpJsonRpcErrorResponse(
            id,
            when (e) {
                is McpException -> e.error
                else -> {
                    onError(e)
                    ErrorMessage.InternalError
                }
            }
        )
    )

    private fun handleRootChanged(session: Session) {
        clientTracking[session]?.let {
            if (it.supportsRoots) {
                val messageId = McpMessageId.random(random)
                it.trackRequest(messageId) { roots.update(McpJson.asA<McpRoot.List.Response.Result>(McpJson.compact(it))) }

                sessions.request(
                    ClientCall(session),
                    McpRoot.List.Request(McpRoot.List.Request.Params(), messageId)
                )
            }
        }
    }

    private fun handleResult(result: JsonRpcResult<McpNodeType>, sessionState: ValidSessionState) = when {
        result.isError() -> Accepted
        else -> with(McpJson) {
            val id = result.id?.let { McpMessageId.parse(compact(it)) }
            when (id) {
                null -> Ok(McpJsonRpcErrorResponse(null, ErrorMessage.ParseError))
                else -> clientTracking[sessionState.session]
                    ?.processResult(id, result.result ?: nullNode())
                    ?.let { Accepted }
                    ?: Unknown
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
                McpLogging.LoggingMessage.Notification(
                    McpLogging.LoggingMessage.Notification.Params(data, level, logger)
                )
            )
        }

        prompts.onChange(context.session) {
            sessions.request(
                context,
                McpPrompt.List.Changed.Notification(McpPrompt.List.Changed.Notification.Params())
            )
        }

        resources.onChange(context.session) {
            sessions.request(
                context,
                McpResource.List.Changed.Notification(McpResource.List.Changed.Notification.Params())
            )
        }

        tools.onChange(context.session) {
            sessions.request(
                context,
                McpTool.List.Changed.Notification(McpTool.List.Changed.Notification.Params())
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

