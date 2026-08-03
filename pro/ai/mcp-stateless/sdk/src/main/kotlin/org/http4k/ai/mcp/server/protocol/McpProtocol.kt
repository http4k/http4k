/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcMessage
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.protocol.messages.McpSubscriptions
import org.http4k.ai.mcp.server.asHttp
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
import org.http4k.ai.mcp.server.protocol.RequestStateCodec.Companion.None
import org.http4k.ai.mcp.server.withServerInfo
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.parse
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.McpFilters
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound
import org.http4k.lens.Header
import org.http4k.lens.MetaKey
import org.http4k.lens.logLevel
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import org.http4k.sse.SseResponse
import java.io.PipedInputStream
import java.io.PipedOutputStream

private const val STREAM_BUFFER_BYTES = 64 * 1024

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
    private val metaData: ServerMetaData,
    private val tools: Tools = tools(),
    private val resources: Resources = resources(),
    private val prompts: Prompts = prompts(),
    completions: Completions = completions(),
    cancellations: Cancellations = cancellations(),
    mcpFilter: McpFilter = McpFilter.NoOp,
    onError: (Throwable) -> Unit = { it.printStackTrace(System.err) },
    requestStateCodec: RequestStateCodec = None,
) : HttpHandler {
    constructor(
        serverMetaData: ServerMetaData,
        vararg capabilities: ServerCapability,
        mcpFilter: McpFilter = McpFilter.NoOp,
    ) : this(
        serverMetaData,
        tools(capabilities.flatMap { it }.filterIsInstance<ToolCapability>()),
        resources(capabilities.flatMap { it }.filterIsInstance<ResourceCapability>()),
        prompts(capabilities.flatMap { it }.filterIsInstance<PromptCapability>()),
        completions(capabilities.flatMap { it }.filterIsInstance<CompletionCapability>()),
        mcpFilter = mcpFilter,
    )

    private val mcpHandler = mcpFilter
        .then(McpFilters.CatchAll(onError))
        .then(RoutingMcpHandler(metaData, completions, prompts, resources, tools, cancellations, requestStateCodec))

    override fun invoke(httpReq: Request): Response {
        val body = httpReq.bodyString()
        val message = runCatching { McpJson.asA<McpJsonRpcRequest>(body) }.getOrNull()
        if (message == null) return errorFor(body).asHttp(metaData.entity)
        validateRequest(message, httpReq, metaData.protocolVersions)?.let { return Ok(it).asHttp(metaData.entity) }
        return when {
            Header.ACCEPT(httpReq)?.accepts(TEXT_EVENT_STREAM) == true -> streamingResponse(message, httpReq)
            else -> dispatch(message, httpReq, FakeSse(httpReq)).asHttp(metaData.entity)
        }
    }

    fun listen(httpReq: Request): SseResponse {
        val body = httpReq.bodyString()
        val message = runCatching { McpJson.asA<McpSubscriptions.Listen.Request>(body) }.getOrNull()
        if (message == null) return errorStream(McpJsonRpcErrorResponse(null, InvalidRequest))
        validateRequest(message, httpReq, metaData.protocolVersions)?.let { return errorStream(it) }

        val filter = message.params.notifications
        val idMeta = subscriptionIdMeta(message.id)
        return SseResponse(OK, subscriptionSseHeaders()) { sse ->
            sse.send(subscriptionEvent(acknowledgement(filter, message.id)))

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

    private fun dispatch(message: McpJsonRpcRequest, httpReq: Request, sse: Sse) =
        mcpHandler(McpRequest(message, httpReq, StreamingClient(sse, MetaKey.logLevel().toLens()(message.meta()))))

    private fun streamingResponse(message: McpJsonRpcRequest, httpReq: Request): Response {
        val out = PipedOutputStream()
        val sse = PipedSse(out, httpReq)
        Thread.ofVirtual().start {
            sse.use {
                when (val response = dispatch(message, httpReq, it)) {
                    is Ok -> it.send(response.message.resultEvent())
                    else -> {}
                }
            }
        }

        return subscriptionSseHeaders().fold(Response(OK)) { r, kv -> r.header(kv.first, kv.second) }
            .body(PipedInputStream(out, STREAM_BUFFER_BYTES))
    }

    private fun errorFor(body: String): McpResponse {
        val payload = runCatching { McpJson.fields(parse(body)).toMap() }
            .getOrElse { return Ok(McpJsonRpcErrorResponse(null, ErrorMessage.ParseError)) }
        val method = payload["method"]?.let { McpJson.text(it) }
        return when (method) {
            null -> Accepted
            !in KNOWN_METHODS -> Ok(McpJsonRpcErrorResponse(payload["id"], MethodNotFound))
            else -> Ok(McpJsonRpcErrorResponse(payload["id"], InvalidRequest))
        }
    }

    private fun errorStream(error: McpJsonRpcErrorResponse) =
        SseResponse(BAD_REQUEST, subscriptionSseHeaders()) { it.send(error.resultEvent()); it.close() }

    private fun McpJsonRpcMessage.resultEvent() =
        SseMessage.Event("message", McpJson.compact(McpJson.asJsonObject(this).withServerInfo(metaData.entity)))
}
