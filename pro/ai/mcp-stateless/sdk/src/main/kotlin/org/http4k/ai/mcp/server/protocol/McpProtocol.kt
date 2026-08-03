/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.PUBLISHED
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.protocol.messages.McpDiscover
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcMessage
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
import org.http4k.ai.mcp.server.withServerInfo
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.parse
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.McpFilters
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound
import org.http4k.sse.SseMessage
import org.http4k.sse.SseResponse

// The request methods this stateless server routes; anything else present-but-unknown -> -32601 (Method Not Found).
private val KNOWN_METHODS = setOf(
    "server/discover",
    "completion/complete",
    "prompts/get",
    "prompts/list",
    "resources/list",
    "resources/read",
    "resources/templates/list",
    "tools/call",
    "tools/list",
    "notifications/cancelled",
    "subscriptions/listen",
)

class McpProtocol(
    val serverInfo: VersionedMcpEntity,
    private val tools: Tools = tools(),
    private val resources: Resources = resources(),
    private val prompts: Prompts = prompts(),
    completions: Completions = completions(),
    cancellations: Cancellations = cancellations(),
    private val supportedVersions: Set<ProtocolVersion> = PUBLISHED,
    discover: () -> McpDiscover.Response.Result = { McpDiscover.Response.Result(supportedVersions.toList()) },
    mcpFilter: McpFilter = McpFilter.NoOp,
    onError: (Throwable) -> Unit = { it.printStackTrace(System.err) },
    requestStateCodec: RequestStateCodec = RequestStateCodec.None,
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
        .then(RoutingMcpHandler(discover, completions, prompts, resources, tools, cancellations, requestStateCodec))

    fun receive(httpReq: Request): McpResponse {
        val body = httpReq.bodyString()
        val message = runCatching { McpJson.asA<McpJsonRpcRequest>(body) }.getOrNull()
        if (message == null) return errorFor(body)
        validateStatelessRequest(message, httpReq, supportedVersions)?.let { return Ok(it) }
        // non-streaming: a FakeSse discards any progress/log the handler emits; only the result returns
        return mcpHandler(McpRequest(message, httpReq, StreamingClient(FakeSse(httpReq), message.logLevel())))
    }

    // asA failed: recover the id + parse/method/invalid distinction with a single node parse (error path only).
    private fun errorFor(body: String): McpResponse {
        val payload = runCatching { McpJson.fields(parse(body)).toMap() }
            .getOrElse { return Ok(McpJsonRpcErrorResponse(null, ErrorMessage.ParseError)) }
        val method = payload["method"]?.let { McpJson.text(it) }
        return when (method) {
            null -> Accepted
            !in KNOWN_METHODS -> Ok(McpJsonRpcErrorResponse(payload["id"], MethodNotFound))
            else -> Ok(McpJsonRpcErrorResponse(payload["id"], ErrorMessage.InvalidRequest))
        }
    }

    /**
     * Streaming request/response: the POST's own response is an SSE stream carrying `notifications/progress`
     * + `notifications/message` (emitted by the handler via its request-scoped [StreamingClient]) followed by
     * the final result as the terminal event, then close. Selected when the client sends `Accept:
     * text/event-stream` (the poly sse face); mirrors the plain [receive] but with a real, live `sse`.
     */
    fun receiveStreaming(httpReq: Request): SseResponse {
        val body = httpReq.bodyString()
        val message = runCatching { McpJson.asA<McpJsonRpcRequest>(body) }.getOrNull()
        if (message == null) return streamErrorFor(body)
        // invalid envelope rejects with a JSON-RPC error event + 400 (regardless of the streaming Accept)
        validateStatelessRequest(message, httpReq, supportedVersions)?.let { return errorStream(BAD_REQUEST, it) }
        return SseResponse(OK, subscriptionSseHeaders()) { sse ->
            when (val response = mcpHandler(McpRequest(message, httpReq, StreamingClient(sse, message.logLevel())))) {
                is Ok -> sse.send(resultEvent(response.message))
                else -> {}
            }
            sse.close()
        }
    }

    // asA failed on the streaming face: return the JSON-RPC error as an event (never a bodiless close).
    private fun streamErrorFor(body: String): SseResponse {
        val payload = runCatching { McpJson.fields(parse(body)).toMap() }
            .getOrElse { return errorStream(OK, McpJsonRpcErrorResponse(null, ErrorMessage.ParseError)) }
        val method = payload["method"]?.let { McpJson.text(it) }
        return when {
            method != null && method !in KNOWN_METHODS ->
                errorStream(NOT_FOUND, McpJsonRpcErrorResponse(payload["id"], MethodNotFound))

            else -> errorStream(OK, McpJsonRpcErrorResponse(payload["id"], ErrorMessage.InvalidRequest))
        }
    }

    private fun errorStream(status: Status, error: McpJsonRpcErrorResponse) =
        SseResponse(status, subscriptionSseHeaders()) { sse -> sse.send(resultEvent(error)); sse.close() }

    // the terminal event of a streaming response: the JSON-RPC result, serverInfo stamped (as the JSON path does)
    private fun resultEvent(message: McpJsonRpcMessage) =
        SseMessage.Event("message", McpJson.compact(McpJson.asJsonObject(message).withServerInfo(serverInfo)))

    fun listen(httpReq: Request): SseResponse {
        val body = httpReq.bodyString()
        val message = runCatching { McpJson.asA<McpSubscriptions.Listen.Request>(body) }.getOrNull()
        if (message == null) return errorStream(BAD_REQUEST, McpJsonRpcErrorResponse(null, ErrorMessage.InvalidRequest))
        validateStatelessRequest(message, httpReq, supportedVersions)?.let { return errorStream(BAD_REQUEST, it) }

        val filter = message.params.notifications
        val idMeta = subscriptionIdMeta(message.id)
        return SseResponse(OK, subscriptionSseHeaders()) { sse ->
                    // ack first, echoing the honored filter (all list-changed types are supported by this server)
                    sse.send(subscriptionEvent(acknowledgement(filter, message.id)))

                    // observers are keyed by the physical stream (`sse`), not the client-chosen subscriptionId
                    // (which isn't unique across clients). Only opted-in types are wired.
                    if (filter.toolsListChanged == true) {
                        tools.onChange(sse) { sse.send(subscriptionEvent(toolsListChanged(idMeta))) }
                    }
                    if (filter.promptsListChanged == true) {
                        prompts.onChange(sse) {
                            sse.send(subscriptionEvent(promptsListChanged(idMeta)))
                        }
                    }
                    if (filter.resourcesListChanged == true) {
                        resources.onChange(sse) {
                            sse.send(subscriptionEvent(resourcesListChanged(idMeta)))
                        }
                    }
                    filter.resourceSubscriptions?.takeIf { it.isNotEmpty() }?.let { uris ->
                        resources.subscribeToUpdates(sse, uris.toSet()) { uri ->
                            sse.send(subscriptionEvent(resourceUpdated(uri, idMeta)))
                        }
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
