/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.http

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMapFailure
import dev.forkhandles.result4k.map
import org.http4k.ai.mcp.CompletionRequest
import org.http4k.ai.mcp.CompletionResponse
import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.McpError.Http
import org.http4k.ai.mcp.McpError.Protocol
import org.http4k.ai.mcp.McpResult
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.client.SubscriptionSpec
import org.http4k.ai.mcp.client.internal.asOrFailure
import org.http4k.ai.mcp.client.internal.serverInfoOrNull
import org.http4k.ai.mcp.client.internal.toCompletionErrorOrFailure
import org.http4k.ai.mcp.client.internal.toElicitationRequest
import org.http4k.ai.mcp.client.internal.toPromptErrorOrFailure
import org.http4k.ai.mcp.client.internal.toResourceErrorOrFailure
import org.http4k.ai.mcp.client.internal.toToolResponseOrError
import org.http4k.ai.mcp.client.toHttpRequest
import org.http4k.ai.mcp.model.ElicitationAction.cancel
import org.http4k.ai.mcp.model.LogLevel
import org.http4k.ai.mcp.model.LogMessage
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Progress
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.model.ResultType
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.ClientProtocolCapability.ElicitationForm
import org.http4k.ai.mcp.protocol.ClientProtocolCapability.ElicitationUrl
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.protocol.messages.McpDiscover
import org.http4k.ai.mcp.protocol.messages.McpElicitation
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.protocol.messages.McpLogging
import org.http4k.ai.mcp.protocol.messages.McpProgress
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpSubscriptions
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.ai.model.ToolName
import org.http4k.client.Http4kSseClient
import org.http4k.client.JavaHttpClient
import org.http4k.client.ReconnectionMode
import org.http4k.client.ReconnectionMode.Immediate
import org.http4k.core.BodyMode.Stream
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.lens.MetaKey
import org.http4k.lens.accept
import org.http4k.lens.logLevel
import org.http4k.lens.progressToken
import org.http4k.sse.SseMessage.Event
import org.http4k.sse.chunkedSseSequence
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

class HttpMcpClient(
    private val baseUri: Uri,
    entity: McpEntity = McpEntity.of("http4k-mcp-client"),
    version: Version = Version.of("0.0.0"),
    private val http: HttpHandler = JavaHttpClient(responseBodyMode = Stream),
    private val protocolVersion: ProtocolVersion = LATEST_VERSION,
    private val capabilities: ClientCapabilities = ClientCapabilities(ElicitationForm, ElicitationUrl), // FIXME is this the correct set? tasks?
    private val subscriptionReconnectMode: ReconnectionMode = Immediate,
) : McpClient {
    private val clientInfo = VersionedMcpEntity(entity, version)
    private val openSubscriptions = CopyOnWriteArrayList<AutoCloseable>()

    private val id = AtomicLong(0)

    private fun nextId() = McpMessageId.of(id.incrementAndGet())

    private fun McpJsonRpcRequest.asHttpRequest() =
        toHttpRequest(protocolVersion, baseUri, clientInfo, capabilities)

    override fun discover(): McpResult<VersionedMcpEntity> =
        http.exchange(McpDiscover.Request(id = nextId())) { node, response ->
            node.serverInfoOrNull()?.let(::Success) ?: Failure(Http(response))
        }

    override fun tools() = object : McpClient.Tools {
        private var lastKnownTools = emptyList<McpTool>()

        override fun list() =
            http.send<McpTool.List.Response.Result>(McpTool.List.Request(McpTool.List.Request.Params(), nextId()))
                .map { it.tools.also { lastKnownTools = it } }

        override fun call(
            name: ToolName, request: ToolRequest,
            onProgress: ((Progress) -> Unit)?, onLog: ((LogMessage) -> Unit)?
        ): McpResult<ToolResponse> {
            val withHeaders = PopulateToolHeaders(lastKnownTools, name, request).then(http)
            val meta = streamingMeta(request.meta, onProgress, onLog)
            val message = McpTool.Call.Request(
                McpTool.Call.Request.Params(
                    name, request.mapValues { McpJson.asJsonObject(it.value) },
                    request.inputResponses.toWire(), request.requestState, meta
                ),
                nextId()
            )
            return withHeaders.send<McpTool.Call.Response.Result>(message, onProgress, onLog)
                .map { toToolResponseOrError(it) }
        }

        override fun onListChanged(handler: () -> Unit) = listenFor { onToolsChanged(handler) }
    }

    override fun prompts() = object : McpClient.Prompts {
        override fun list() =
            http.send<McpPrompt.List.Response.Result>(McpPrompt.List.Request(McpPrompt.List.Request.Params(), nextId()))
                .map { it.prompts }

        override fun get(
            name: PromptName, request: PromptRequest,
            onProgress: ((Progress) -> Unit)?, onLog: ((LogMessage) -> Unit)?
        ): McpResult<PromptResponse> {
            val meta = streamingMeta(request.meta, onProgress, onLog)
            val message = McpPrompt.Get.Request(
                McpPrompt.Get.Request.Params(name, request, request.inputResponses.toWire(), request.requestState, meta),
                nextId()
            )
            return http
                .send<McpPrompt.Get.Response.Result>(message, onProgress, onLog)
                .map { it.toPromptResponse() }
                .flatMapFailure { toPromptErrorOrFailure(it) }
        }

        override fun onListChanged(handler: () -> Unit) = listenFor { onPromptsChanged(handler) }
    }

    override fun resources() = object : McpClient.Resources {
        override fun list() =
            http.send<McpResource.List.Response.Result>(McpResource.List.Request(McpResource.List.Request.Params(), nextId()))
                .map { it.resources }

        override fun listTemplates() =
            http.send<McpResource.ListTemplates.Response.Result>(
                McpResource.ListTemplates.Request(McpResource.ListTemplates.Request.Params(), nextId())
            ).map { it.resourceTemplates }

        override fun read(
            request: ResourceRequest,
            onProgress: ((Progress) -> Unit)?, onLog: ((LogMessage) -> Unit)?
        ): McpResult<ResourceResponse> {
            val meta = streamingMeta(request.meta, onProgress, onLog)
            val message = McpResource.Read.Request(
                McpResource.Read.Request.Params(request.uri, request.inputResponses.toWire(), request.requestState, meta),
                nextId()
            )
            return http
                .send<McpResource.Read.Response.Result>(message, onProgress, onLog)
                .map { it.toResourceResponse() }
                .flatMapFailure { toResourceErrorOrFailure(it) }
        }

        override fun onListChanged(handler: () -> Unit) = listenFor { onResourcesChanged(handler) }
        override fun subscribe(uri: Uri, handler: () -> Unit) = listenFor { onResourceUpdated(uri, handler) }
    }

    override fun completions() = object : McpClient.Completions {
        override fun complete(ref: Reference, request: CompletionRequest) =
            http.send<McpCompletion.Response.Result>(
                McpCompletion.Request(McpCompletion.Request.Params(ref, request.argument), nextId())
            )
                .map { it.completion.run { CompletionResponse.Ok(values, total, hasMore) as CompletionResponse } }
                .flatMapFailure { toCompletionErrorOrFailure(it) }
    }

    private fun listen(spec: SubscriptionSpec): McpResult<AutoCloseable> {
        val listenReq = McpSubscriptions.Listen.Request(
            McpSubscriptions.Listen.Request.Params(spec.toFilter()), nextId()
        ).asHttpRequest()
        val sseClient = Http4kSseClient(listenReq, http, subscriptionReconnectMode)

        thread(isDaemon = true) {
            sseClient.received()
                .filterIsInstance<Event>()
                .filter { it.event == "message" }
                .forEach { dispatch(it.data, spec) }
        }

        val subscription = object : AutoCloseable {
            override fun close() {
                sseClient.close()
                openSubscriptions.remove(this)
            }
        }
        openSubscriptions += subscription
        return Success(subscription)
    }

    private fun listenFor(build: SubscriptionSpec.() -> Unit) = listen(SubscriptionSpec().apply(build))

    override fun close() = openSubscriptions.toList().forEach { it.close() }

    private fun dispatch(data: String, spec: SubscriptionSpec) {
        when (val message = runCatching { McpJson.asA<McpJsonRpcRequest>(data) }.getOrNull()) {
            is McpTool.List.Changed.Notification -> spec.toolsHandlers.forEach { it() }
            is McpPrompt.List.Changed.Notification -> spec.promptsHandlers.forEach { it() }
            is McpResource.List.Changed.Notification -> spec.resourcesHandlers.forEach { it() }
            is McpResource.Updated.Notification -> spec.resourceHandlers[message.params.uri]?.forEach { it() }
            else -> {}
        }
    }

    // progress/log ride the request's own _meta: a generated progressToken lets the server emit progress,
    // logLevel=debug asks for all logs. Only stamped when the caller wants notifications.
    private fun streamingMeta(base: Meta, onProgress: ((Progress) -> Unit)?, onLog: ((LogMessage) -> Unit)?): Meta {
        var meta = base
        if (onProgress != null) meta = MetaKey.progressToken<Any>().toLens()(nextId().value, meta)
        if (onLog != null) meta = MetaKey.logLevel().toLens()(LogLevel.debug, meta)
        return meta
    }

    private inline fun <T> HttpHandler.exchange(
        message: McpJsonRpcRequest,
        onSuccess: (McpNodeType, Response) -> McpResult<T>
    ): McpResult<T> {
        val response = this(message.asHttpRequest())
        return when {
            response.status.successful -> onSuccess(McpJson.parse(response.bodyString()), response)
            else -> Failure(Http(response))
        }
    }

    // Streaming when progress/log callbacks are present: send Accept: text/event-stream, and if the server
    // streams, read the response body as an SSE sequence — diverting progress/message notifications to the
    // callbacks and taking the final (method-less) event as the result. Otherwise it's a single JSON body.
    private inline fun <reified T : Any> HttpHandler.send(
        message: McpJsonRpcRequest,
        noinline onProgress: ((Progress) -> Unit)? = null,
        noinline onLog: ((LogMessage) -> Unit)? = null
    ): McpResult<T> {
        val streaming = onProgress != null || onLog != null
        val response = this(message.asHttpRequest().let { if (streaming) it.accept(TEXT_EVENT_STREAM) else it })
        return when {
            response.header("content-type")?.contains(TEXT_EVENT_STREAM.value, true) == true ->
                response.readStreamingResult(onProgress, onLog)

            // a JSON-RPC error body is meaningful regardless of HTTP status: 2026-07-28 returns
            // validation/method errors as 4xx, so parse the body first and only fall back to Http.
            else -> runCatching { McpJson.parse(response.bodyString()).asOrFailure<T>() }
                .getOrElse { Failure(Http(response)) }
                .flatMapFailure { if (it is Protocol) Failure(it) else Failure(Http(response)) }
        }
    }

    private inline fun <reified T : Any> Response.readStreamingResult(
        noinline onProgress: ((Progress) -> Unit)?,
        noinline onLog: ((LogMessage) -> Unit)?
    ): McpResult<T> {
        val result = body.stream.chunkedSseSequence().filterIsInstance<Event>()
            .firstNotNullOfOrNull { event ->
                val node = McpJson.parse(event.data)
                when ((node as? MoshiObject)?.get("method")) {
                    null -> node
                    else -> null.also { dispatchNotification(event.data, onProgress, onLog) }
                }
            }
        return when (result) {
            null -> Failure(Protocol(ErrorMessage(-1, "streaming response ended with no result")))
            else -> result.asOrFailure()
        }
    }

    private fun dispatchNotification(data: String, onProgress: ((Progress) -> Unit)?, onLog: ((LogMessage) -> Unit)?) {
        when (val m = runCatching { McpJson.asA<McpJsonRpcRequest>(data) }.getOrNull()) {
            is McpProgress.Notification ->
                onProgress?.invoke(Progress(m.params.progressToken, m.params.progress, m.params.total, m.params.description))

            is McpLogging.LoggingMessage.Notification ->
                onLog?.invoke(LogMessage(m.params.data, m.params.level, m.params.logger))

            else -> {}
        }
    }
}

private fun McpPrompt.Get.Response.Result.toPromptResponse(): PromptResponse = when (resultType) {
    ResultType.input_required -> PromptResponse.InputRequired(
        inputRequests.orEmpty().mapValues { it.value.toElicitationRequest() }, requestState
    )

    else -> PromptResponse.Ok(messages, description, ttlMs)
}

private fun McpResource.Read.Response.Result.toResourceResponse(): ResourceResponse = when (resultType) {
    ResultType.input_required -> ResourceResponse.InputRequired(
        inputRequests.orEmpty().mapValues { it.value.toElicitationRequest() }, requestState
    )

    else -> ResourceResponse.Ok(contents, ttlMs)
}

private fun Map<String, ElicitationResponse>.toWire() = takeIf { it.isNotEmpty() }?.mapValues {
    when (val response = it.value) {
        is ElicitationResponse.Ok -> McpElicitation.Result(response.action, response.content)
        is ElicitationResponse.Error -> McpElicitation.Result(cancel)
    }
}
