/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.InitializeHandler
import org.http4k.ai.mcp.model.LogLevel.error
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.protocol.messages.McpLogging
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
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
import org.http4k.ai.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.ai.mcp.server.protocol.McpResponse.Accepted
import org.http4k.ai.mcp.server.protocol.McpResponse.Ok
import org.http4k.ai.mcp.server.protocol.McpResponse.Unknown
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.parse
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.core.Request
import org.http4k.filter.McpFilters
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

        val method = McpJson.textValueOf(rawPayload, "method")
        val payload = McpJson.fields(rawPayload).toMap()

        return when {
            method != null -> {
                val message = runCatching { McpJson.asA<McpJsonRpcRequest>(body) }
                    .getOrElse { return Ok(McpJsonRpcErrorResponse(payload["id"], ErrorMessage.InvalidRequest)) }

                val mcpHandler = mcpFilter
                    .then(AssignAndCloseSession(sessions, transport))
                    .then(McpFilters.CatchAll(onError))
                    .then(handler)

                mcpHandler(McpRequest(sessionState.session, message, httpReq))
            }

            else -> handleResult(JsonRpcResult(McpJson, payload), sessionState)
        }
    }

    private val handler = RoutingMcpHandler(
        initializer = initializer,
        clientTracking = clientTracking,
        completions = completions,
        prompts = prompts,
        resources = resources,
        tools = tools,
        logger = logger,
        tasks = tasks,
        cancellations = cancellations,
        roots = roots,
        random = random,
        sessions = sessions,
    )

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

    fun retrieveSession(req: Request) = sessions.retrieveSession(req)

    fun unsubscribe(context: Subscription) {
        clientTracking.remove(context.session)
        sessions.end(context)
    }

    fun subscribe(context: Subscription, transport: Transport, connectRequest: Request) {
        sessions.assign(context, transport, connectRequest)

        logger.subscribe(context.session, error) { data, level, logger ->
            sessions.send(
                context,
                McpLogging.LoggingMessage.Notification(
                    McpLogging.LoggingMessage.Notification.Params(data, level, logger)
                )
            )
        }

        prompts.onChange(context.session) {
            sessions.send(
                context,
                McpPrompt.List.Changed.Notification(McpPrompt.List.Changed.Notification.Params())
            )
        }

        resources.onChange(context.session) {
            sessions.send(
                context,
                McpResource.List.Changed.Notification(McpResource.List.Changed.Notification.Params())
            )
        }

        tools.onChange(context.session) {
            sessions.send(
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

