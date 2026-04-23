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
import org.http4k.ai.mcp.protocol.messages.ServerMessage
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
            .getOrElse { return Ok(ErrorMessage.ParseError.toJsonRpc(null)) }

        val payload = McpJson.fields(rawPayload).toMap()

        return if (payload["method"] != null) {
            val message = runCatching { McpJson.asA<McpJsonRpcRequest>(body) }
                .getOrElse { return Ok(ErrorMessage.InvalidRequest.toJsonRpc(payload["id"])) }
            responseFor(McpRequest(sessionState.session, message, httpReq), transport)
        } else {
            handleResult(JsonRpcResult(McpJson, payload), sessionState)
        }
    }

    private fun responseFor(mcpRequest: McpRequest, transport: Transport): McpResponse {
        val filter = mcpFilter.then(AssignAndCloseSession(sessions, transport))

        return filter.then { mcp ->
            val msg = mcp.message
            runCatching {
                when (msg) {
                    is McpInitialize.Request -> respond(msg) { handleInitialize(msg.params, mcp.http, mcp.session) }
                    is McpPing.Request -> respond(msg) { ServerMessage.Response.Empty }
                    is McpCompletion.Request -> respond(msg) { completions.complete(msg.params, clientFor(mcp.session), mcp.http) }
                    is McpPrompt.Get.Request -> respond(msg) { prompts.get(msg.params, clientFor(mcp.session), mcp.http) }
                    is McpPrompt.List.Request -> respond(msg) { prompts.list(msg.params, clientFor(mcp.session), mcp.http) }
                    is McpResource.ListTemplates.Request -> respond(msg) { resources.listTemplates(msg.params, clientFor(mcp.session), mcp.http) }
                    is McpResource.List.Request -> respond(msg) { resources.listResources(msg.params, clientFor(mcp.session), mcp.http) }
                    is McpResource.Read.Request -> respond(msg) { resources.read(msg.params, clientFor(mcp.session), mcp.http) }
                    is McpResource.Subscribe.Request -> respond(msg) {
                        when (resources) {
                            is ObservableResources -> resources.subscribe(mcp.session, msg.params) {
                                sessions.request(
                                    Subscription(mcp.session),
                                    McpResource.Updated.Notification.Params(msg.params.uri).toJsonRpc(McpResource.Updated)
                                )
                            }
                        }
                        ServerMessage.Response.Empty
                    }
                    is McpResource.Unsubscribe.Request -> respond(msg) {
                        when (resources) {
                            is ObservableResources -> resources.unsubscribe(mcp.session, msg.params)
                        }
                        ServerMessage.Response.Empty
                    }
                    is McpLogging.SetLevel.Request -> respond(msg) {
                        logger.setLevel(mcp.session, msg.params.level)
                        ServerMessage.Response.Empty
                    }
                    is McpTool.Call.Request -> respond(msg) { tools.call(msg.params, clientFor(mcp.session), mcp.http) }
                    is McpTool.List.Request -> respond(msg) { tools.list(msg.params, clientFor(mcp.session), mcp.http) }
                    is McpTask.Get.Request -> respond(msg) { tasks.get(mcp.session, msg.params, clientFor(mcp.session), mcp.http) }
                    is McpTask.Result.Request -> respond(msg) { tasks.result(mcp.session, msg.params, clientFor(mcp.session), mcp.http) }
                    is McpTask.Cancel.Request -> respond(msg) { tasks.cancel(mcp.session, msg.params, clientFor(mcp.session), mcp.http) }
                    is McpTask.List.Request -> respond(msg) { tasks.list(mcp.session, msg.params, clientFor(mcp.session), mcp.http) }

                    is McpInitialize.Initialized.Notification -> Accepted
                    is McpProgress.Notification -> Accepted
                    is McpCancelled.Notification -> { cancellations.cancel(msg.params); Accepted }
                    is McpTask.Status.Notification -> { tasks.update(mcp.session, msg.params); Accepted }
                    is McpRoot.Changed.Notification -> { handleRootChanged(mcp.session, transport); Accepted }
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
            }.getOrElse { handleError(it, msg.id) }
        }(mcpRequest)
    }

    private fun respond(msg: McpJsonRpcRequest, fn: () -> ServerMessage.Response) =
        Ok(fn().toJsonRpc(msg.id))

    private fun handleError(e: Throwable, id: McpNodeType?): McpResponse = Ok(
        when (e) {
            is McpException -> e.error.toJsonRpc(id)
            else -> {
                onError(e)
                ErrorMessage.InternalError.toJsonRpc(id)
            }
        }
    )

    private fun handleRootChanged(session: Session, transport: Transport) {
        clientTracking[session]?.let {
            if (it.supportsRoots) {
                val messageId = McpMessageId.random(random)
                it.trackRequest(messageId) { roots.update(McpJson.asA<McpRoot.List.Response.Result>(McpJson.compact(it))) }

                sessions.respond(
                    transport,
                    ClientCall(session),
                    McpRoot.List.Request.Params().toJsonRpc(McpRoot.List, asJsonObject(messageId))
                )
            }
        }
    }

    private fun handleResult(result: JsonRpcResult<McpNodeType>, sessionState: ValidSessionState) = when {
        result.isError() -> Accepted
        else -> with(McpJson) {
            val id = result.id?.let { McpMessageId.parse(compact(it)) }
            when (id) {
                null -> Ok(ErrorMessage.ParseError.toJsonRpc(null))
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

