/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.protocol.messages.McpDiscover
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.protocol.messages.McpSubscriptions
import org.http4k.ai.mcp.server.capability.CompletionCapability
import org.http4k.ai.mcp.server.capability.PromptCapability
import org.http4k.ai.mcp.server.capability.ResourceCapability
import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.ai.mcp.server.capability.cancellations
import org.http4k.ai.mcp.server.capability.completions
import org.http4k.ai.mcp.server.capability.prompts
import org.http4k.ai.mcp.server.capability.resources
import org.http4k.ai.mcp.server.capability.tools
import org.http4k.ai.mcp.server.protocol.McpResponse.Accepted
import org.http4k.ai.mcp.server.protocol.McpResponse.Ok
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.parse
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.McpFilters
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.sse.SseResponse

/**
 * The stateless (2026-07-28) MCP protocol: each request is self-describing and independent — no
 * session, no handshake, no server->client channel. [receive] turns one HTTP request into one response.
 */
class McpProtocol(
    val serverInfo: VersionedMcpEntity,
    private val tools: Tools = tools(),
    private val resources: Resources = resources(),
    private val prompts: Prompts = prompts(),
    completions: Completions = completions(),
    cancellations: Cancellations = cancellations(),
    supportedVersions: Set<ProtocolVersion> = ProtocolVersion.PUBLISHED,
    discover: () -> McpDiscover.Response.Result = { McpDiscover.Response.Result(supportedVersions.toList()) },
    mcpFilter: McpFilter = McpFilter.NoOp,
    onError: (Throwable) -> Unit = { it.printStackTrace(System.err) },
) {
    constructor(
        metaData: ServerMetaData,
        vararg capabilities: ServerCapability,
        mcpFilter: McpFilter = McpFilter.NoOp,
    ) : this(
        metaData.entity,
        tools(capabilities.flatMap { it }.filterIsInstance<ToolCapability>()),
        resources(capabilities.flatMap { it }.filterIsInstance<ResourceCapability>()),
        prompts(capabilities.flatMap { it }.filterIsInstance<PromptCapability>()),
        completions(capabilities.flatMap { it }.filterIsInstance<CompletionCapability>()),
        supportedVersions = metaData.protocolVersions,
        discover = { discoverResultFor(metaData) },
        mcpFilter = mcpFilter,
    )

    private val mcpHandler = mcpFilter
        .then(McpFilters.CatchAll(onError))
        .then(ValidateProtocolVersion(supportedVersions))
        .then(RoutingMcpHandler(discover, completions, prompts, resources, tools, cancellations))

    fun receive(httpReq: Request): McpResponse {
        val body = httpReq.bodyString()
        val rawPayload = runCatching { parse(body) }
            .getOrElse { return Ok(McpJsonRpcErrorResponse(null, ErrorMessage.ParseError)) }
        val payload = McpJson.fields(rawPayload).toMap()

        return when {
            payload["method"] != null -> {
                val message = runCatching { McpJson.asA<McpJsonRpcRequest>(body) }
                    .getOrElse { return Ok(McpJsonRpcErrorResponse(payload["id"], ErrorMessage.InvalidRequest)) }
                mcpHandler(McpRequest(message, httpReq))
            }

            else -> Accepted
        }
    }

    /**
     * Stateless `subscriptions/listen`: one long-lived SSE stream whose lifetime IS the subscription.
     * Sends the acknowledgement first (honored subset, tagged with subscriptionId = the request id), then
     * holds the stream open for change notifications. No session, no replay — on drop the client re-listens.
     */
    fun listen(httpReq: Request): SseResponse {
        val message = runCatching { McpJson.asA<McpSubscriptions.Listen.Request>(httpReq.bodyString()) }
            .getOrNull() ?: return SseResponse(BAD_REQUEST) { it.close() }

        val filter = message.params.notifications
        val idMeta = subscriptionIdMeta(message.id)
        return SseResponse(OK, subscriptionSseHeaders()) { sse ->
            // ack first, echoing the honored filter (all list-changed types are supported by this server)
            sse.send(subscriptionEvent(acknowledgement(filter, message.id)))

            // observers are keyed by the physical stream (`sse`), not the client-chosen subscriptionId
            // (which isn't unique across clients). Only opted-in types are wired.
            if (filter.toolsListChanged == true) tools.onChange(sse) { sse.send(subscriptionEvent(toolsListChanged(idMeta))) }
            if (filter.promptsListChanged == true) prompts.onChange(sse) { sse.send(subscriptionEvent(promptsListChanged(idMeta))) }
            if (filter.resourcesListChanged == true) resources.onChange(sse) { sse.send(subscriptionEvent(resourcesListChanged(idMeta))) }
            filter.resourceSubscriptions?.takeIf { it.isNotEmpty() }?.let { uris ->
                resources.subscribeToUpdates(sse, uris.toSet()) { uri -> sse.send(subscriptionEvent(resourceUpdated(uri, idMeta))) }
            }

            sse.onClose {
                tools.removeObserver(sse)
                prompts.removeObserver(sse)
                resources.removeObserver(sse)
                resources.removeUpdateSubscriber(sse)
            }
        }
    }
}
