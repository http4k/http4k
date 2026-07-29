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
import org.http4k.ai.mcp.ElicitationHandler
import org.http4k.ai.mcp.ElicitationRequest
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
import org.http4k.ai.mcp.client.internal.toPromptErrorOrFailure
import org.http4k.ai.mcp.client.internal.toResourceErrorOrFailure
import org.http4k.ai.mcp.client.internal.toToolResponseOrError
import org.http4k.ai.mcp.client.toHttpRequest
import org.http4k.ai.mcp.model.ElicitationAction.cancel
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.McpMessageId
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
import org.http4k.ai.mcp.protocol.messages.HasInputRequired
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.protocol.messages.McpDiscover
import org.http4k.ai.mcp.protocol.messages.McpElicitation
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
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
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.sse.SseMessage.Event
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.toList
import kotlin.concurrent.thread

class HttpMcpClient(
    private val baseUri: Uri,
    entity: McpEntity = McpEntity.of("http4k-mcp-client"),
    version: Version = Version.of("0.0.0"),
    private val http: HttpHandler = JavaHttpClient(responseBodyMode = Stream),
    private val protocolVersion: ProtocolVersion = LATEST_VERSION,
    private val onElicitation: ElicitationHandler = { ElicitationResponse.Ok(cancel) },
    private val capabilities: ClientCapabilities = ClientCapabilities(ElicitationForm, ElicitationUrl),
    private val subscriptionReconnectMode: ReconnectionMode = Immediate,
) : McpClient {
    private val clientInfo = VersionedMcpEntity(entity, version)
    private val openSubscriptions = CopyOnWriteArrayList<AutoCloseable>()

    private val id = AtomicLong(0)
    private fun nextId() = McpMessageId.of(id.incrementAndGet())

    private fun McpJsonRpcRequest.asHttpRequest() =
        toHttpRequest(protocolVersion, baseUri, clientInfo, capabilities)

    override fun start(overrideDefaultTimeout: Duration?): McpResult<Unit> = Success(Unit)

    override fun discover(overrideDefaultTimeout: Duration?): McpResult<VersionedMcpEntity> =
        http.exchange(McpDiscover.Request(id = nextId())) { node, response ->
            node.serverInfoOrNull()?.let(::Success) ?: Failure(Http(response))
        }

    override fun tools() = object : McpClient.Tools {
        private var lastKnownTools = emptyList<McpTool>()

        override fun list(overrideDefaultTimeout: Duration?) =
            http.send<McpTool.List.Response.Result>(McpTool.List.Request(McpTool.List.Request.Params(), nextId()))
                .map { it.tools.also { lastKnownTools = it } }

        override fun call(name: ToolName, request: ToolRequest, overrideDefaultTimeout: Duration?): McpResult<ToolResponse> {
            val withHeaders = PopulateToolHeaders(lastKnownTools, name, request).then(http)
            return mrtrLoop<McpTool.Call.Response.Result>(withHeaders) { inputResponses, requestState ->
                McpTool.Call.Request(
                    McpTool.Call.Request.Params(
                        name, request.mapValues { McpJson.asJsonObject(it.value) }, inputResponses, requestState
                    ),
                    nextId()
                )
            }.map { toToolResponseOrError(it) }
        }

        override fun onListChanged(handler: () -> Unit) = listenFor { onToolsChanged(handler) }
    }

    override fun prompts() = object : McpClient.Prompts {
        override fun list(overrideDefaultTimeout: Duration?) =
            http.send<McpPrompt.List.Response.Result>(McpPrompt.List.Request(McpPrompt.List.Request.Params(), nextId()))
                .map { it.prompts }

        override fun get(name: PromptName, request: PromptRequest, overrideDefaultTimeout: Duration?) =
            mrtrLoop<McpPrompt.Get.Response.Result>(
                PopulateMcpHeaders(name.value).then(http)
            ) { inputResponses, requestState ->
                McpPrompt.Get.Request(
                    McpPrompt.Get.Request.Params(name, request, inputResponses, requestState), nextId()
                )
            }
                .map { PromptResponse.Ok(it.messages, it.description, it.ttlMs) as PromptResponse }
                .flatMapFailure { toPromptErrorOrFailure(it) }

        override fun onListChanged(handler: () -> Unit) = listenFor { onPromptsChanged(handler) }
    }

    override fun resources() = object : McpClient.Resources {
        override fun list(overrideDefaultTimeout: Duration?) =
            http.send<McpResource.List.Response.Result>(McpResource.List.Request(McpResource.List.Request.Params(), nextId()))
                .map { it.resources }

        override fun listTemplates(overrideDefaultTimeout: Duration?) =
            http.send<McpResource.ListTemplates.Response.Result>(
                McpResource.ListTemplates.Request(McpResource.ListTemplates.Request.Params(), nextId())
            ).map { it.resourceTemplates }

        override fun read(request: ResourceRequest, overrideDefaultTimeout: Duration?) =
            mrtrLoop<McpResource.Read.Response.Result>(
                PopulateMcpHeaders(request.uri.toString()).then(http)
            ) { inputResponses, requestState ->
                McpResource.Read.Request(
                    McpResource.Read.Request.Params(request.uri, inputResponses, requestState), nextId()
                )
            }
                .map { ResourceResponse.Ok(it.contents, it.ttlMs) as ResourceResponse }
                .flatMapFailure { toResourceErrorOrFailure(it) }

        override fun onListChanged(handler: () -> Unit) = listenFor { onResourcesChanged(handler) }
        override fun subscribe(uri: Uri, handler: () -> Unit) = listenFor { onResourceUpdated(uri, handler) }
    }

    override fun completions() = object : McpClient.Completions {
        override fun complete(ref: Reference, request: CompletionRequest, overrideDefaultTimeout: Duration?) =
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

    private inline fun <reified T> mrtrLoop(
        noinline httpWithHeaders: HttpHandler,
        buildRequest: (Map<String, McpElicitation.Result>?, String?) -> McpJsonRpcRequest,
    ): McpResult<T> where T : Any, T : HasInputRequired {
        var inputResponses: Map<String, McpElicitation.Result>? = null
        var requestState: String? = null
        repeat(MAX_MRTR_ROUNDS) {
            val outcome = httpWithHeaders.send<T>(buildRequest(inputResponses, requestState))
            val result = when (outcome) {
                is Failure -> return outcome
                is Success -> outcome.value
            }
            if (result.resultType != ResultType.input_required) return outcome
            inputResponses = result.inputRequests.orEmpty()
                .mapValues { (_, create) -> onElicitation(create.toElicitationRequest()).toWire() }
            requestState = result.requestState
        }
        return Failure(Protocol(ErrorMessage(-1, "MRTR did not complete within $MAX_MRTR_ROUNDS rounds")))
    }

    // POST the message; on a successful status parse the body and delegate extraction to [onSuccess],
    // otherwise fail with the raw HTTP response. Shared by send() and discover() (which read different
    // things out of the body — the typed result vs the serverInfo in _meta before it is stripped).
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

    private inline fun <reified T : Any> HttpHandler.send(message: McpJsonRpcRequest): McpResult<T> =
        exchange(message) { node, response ->
            node.asOrFailure<T>().flatMapFailure { if (it is Protocol) Failure(it) else Failure(Http(response)) }
        }
}

private const val MAX_MRTR_ROUNDS = 8

private fun McpElicitation.Create.toElicitationRequest(): ElicitationRequest = when (val p = params) {
    is McpElicitation.Create.Params.Form -> ElicitationRequest.Form(p.message, p.requestedSchema)
    is McpElicitation.Create.Params.Url -> ElicitationRequest.Url(p.message, p.url)
}

private fun ElicitationResponse.toWire(): McpElicitation.Result = when (this) {
    is ElicitationResponse.Ok -> McpElicitation.Result(action, content)
    is ElicitationResponse.Error -> McpElicitation.Result(cancel)
}
