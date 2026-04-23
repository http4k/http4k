/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.http

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.flatMapFailure
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.resultFrom
import org.http4k.ai.mcp.CompletionRequest
import org.http4k.ai.mcp.CompletionResponse
import org.http4k.ai.mcp.ElicitationHandler
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.ElicitationResponse.Ok
import org.http4k.ai.mcp.ElicitationResponse.Task
import org.http4k.ai.mcp.McpError
import org.http4k.ai.mcp.McpError.Http
import org.http4k.ai.mcp.McpResult
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.SamplingHandler
import org.http4k.ai.mcp.SamplingRequest
import org.http4k.ai.mcp.SamplingResponse
import org.http4k.ai.mcp.SamplingResponse.Error
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.client.asAOrFailure
import org.http4k.ai.mcp.client.internal.McpCallback
import org.http4k.ai.mcp.client.internal.toCompletionErrorOrFailure
import org.http4k.ai.mcp.client.internal.toPromptErrorOrFailure
import org.http4k.ai.mcp.client.internal.toResourceErrorOrFailure
import org.http4k.ai.mcp.client.internal.toToolElicitationRequiredOrError
import org.http4k.ai.mcp.client.internal.toToolResponseOrError
import org.http4k.ai.mcp.client.toHttpRequest
import org.http4k.ai.mcp.model.ElicitationId
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Progress
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.model.TaskId
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.ClientCapabilities.Companion.All
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.protocol.messages.DomainError
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.protocol.messages.McpElicitations
import org.http4k.ai.mcp.protocol.messages.McpInitialize
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcMessage
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.protocol.messages.McpProgress
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpSampling
import org.http4k.ai.mcp.protocol.messages.McpTask
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.asA
import org.http4k.ai.mcp.util.McpJson.compact
import org.http4k.ai.model.ToolName
import org.http4k.client.Http4kSseClient
import org.http4k.client.JavaHttpClient
import org.http4k.client.ReconnectionMode
import org.http4k.client.ReconnectionMode.Immediate
import org.http4k.core.BodyMode.Stream
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.MoshiObject
import org.http4k.lens.Header
import org.http4k.lens.MCP_PROTOCOL_VERSION
import org.http4k.lens.MCP_SESSION_ID
import org.http4k.lens.MetaKey
import org.http4k.lens.accept
import org.http4k.lens.progressToken
import org.http4k.sse.SseMessage.Event
import org.http4k.sse.chunkedSseSequence
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.Long.Companion.MAX_VALUE
import kotlin.concurrent.thread

/**
 * HTTP Streaming connection MCP client
 */
class HttpStreamingMcpClient(
    private val baseUri: Uri,
    private val name: McpEntity = McpEntity.of("http4k-mcp-client"),
    private val version: Version = Version.of("0.0.0"),
    private val http: HttpHandler = JavaHttpClient(responseBodyMode = Stream),
    private val capabilities: ClientCapabilities = All,
    private val protocolVersion: ProtocolVersion = LATEST_VERSION,
    private val notificationSseReconnectionMode: ReconnectionMode = Immediate
) : McpClient {
    private val callbacks = mutableMapOf<McpRpcMethod, MutableList<McpCallback<*>>>()

    private val _sessionId = AtomicReference<SessionId>()

    override val sessionId get() = _sessionId.get()

    private val id = AtomicLong(0)

    private fun nextId() = McpMessageId.of(id.incrementAndGet())

    override fun start(overrideDefaultTimeout: Duration?) = http.send(
        McpInitialize.Request(
            McpInitialize.Request.Params(
                VersionedMcpEntity(name, version),
                capabilities,
                protocolVersion
            ), nextId()
        )
    )
        .flatMap { it.first().asAOrFailure<McpInitialize.Response.Result>() }
        .also {
            val latch = CountDownLatch(1)
            thread(isDaemon = true) {
                Http4kSseClient(
                    Request(GET, baseUri)
                        .with(Header.MCP_SESSION_ID of sessionId)
                        .with(Header.MCP_PROTOCOL_VERSION of protocolVersion),
                    http, notificationSseReconnectionMode, System.err::println
                )
                    .received()
                    .filterIsInstance<Event>()
                    .filter { it.event == "message" }
                    .map {
                        latch.countDown()
                        resultFrom { McpJson.parse(it.data) as MoshiObject }
                    }
                    .filterIsInstance<Success<MoshiObject>>()
                    .map { it.value }
                    .filter { it["method"] != null }
                    .forEach { obj ->
                        val message = asA<McpJsonRpcRequest>(compact(obj))
                        val id = message.id?.let { idVal -> asA<McpMessageId>(compact(McpJson.asJsonObject(idVal))) }
                        callbacks[message.method]?.any { it(message, id) }
                    }
            }

            latch.await(overrideDefaultTimeout?.toMillis() ?: MAX_VALUE, MILLISECONDS)
        }


    override fun progress() = object : McpClient.RequestProgress {
        override fun onProgress(fn: (Progress) -> Unit) {
            callbacks.getOrPut(McpProgress.Method) { mutableListOf() }.add(
                McpCallback(McpProgress.Notification.Params::class) { n, _ ->
                    fn(Progress(n.progressToken, n.progress, n.total, n.description))
                }
            )
        }
    }

    override fun tools() = object : McpClient.Tools {
        override fun onChange(fn: () -> Unit) {
            callbacks.getOrPut(McpTool.List.Changed.Method) { mutableListOf() }.add(
                McpCallback(McpPrompt.List.Changed.Notification.Params::class) { _, _ -> fn() }
            )
        }

        override fun list(overrideDefaultTimeout: Duration?) =
            http.send(McpTool.List.Request(McpTool.List.Request.Params(), nextId()))
                .flatMap { it.first().asAOrFailure<McpTool.List.Response.Result>() }
                .map { it.tools }

        override fun call(
            name: ToolName,
            request: ToolRequest,
            overrideDefaultTimeout: Duration?
        ): Result<ToolResponse, McpError> {
            val incoming = http.send(
                McpTool.Call.Request(
                    McpTool.Call.Request.Params(
                        name,
                        request.mapValues { McpJson.asJsonObject(it.value) },
                        request.meta
                    ), nextId()
                )
            )
            return incoming
                .flatMap {
                    it.mapNotNull {
                        when ((McpJson.parse(it.data) as MoshiObject)["method"]) {
                            null -> it
                            else -> {
                                val message = asA<McpJsonRpcRequest>(it.data)
                                val id = message.id?.let { idVal -> asA<McpMessageId>(compact(McpJson.asJsonObject(idVal))) }
                                callbacks[message.method]?.any { it(message, id) }
                                null
                            }
                        }
                    }.first().asAOrFailure<McpTool.Call.Response.Result>()
                }
                .map { toToolResponseOrError(it) }
                .flatMapFailure { toToolElicitationRequiredOrError(it) }
        }
    }

    override fun prompts() = object : McpClient.Prompts {
        override fun onChange(fn: () -> Unit) {
            callbacks.getOrPut(McpPrompt.List.Changed.Method) { mutableListOf() }.add(
                McpCallback(McpPrompt.List.Changed.Notification.Params::class) { _, _ -> fn() }
            )
        }

        override fun list(overrideDefaultTimeout: Duration?) =
            http.send(McpPrompt.List.Request(McpPrompt.List.Request.Params(), nextId()))
                .flatMap { it.first().asAOrFailure<McpPrompt.List.Response.Result>() }
                .map { it.prompts }

        override fun get(
            name: PromptName,
            request: PromptRequest,
            overrideDefaultTimeout: Duration?
        ) = http.send(McpPrompt.Get.Request(McpPrompt.Get.Request.Params(name, request), nextId()))
            .flatMap { it.first().asAOrFailure<McpPrompt.Get.Response.Result>() }
            .map { PromptResponse.Ok(it.messages, it.description) as PromptResponse }
            .flatMapFailure { toPromptErrorOrFailure(it) }
    }

    override fun elicitations() = object : McpClient.Elicitations {
        override fun onElicitation(overrideDefaultTimeout: Duration?, fn: ElicitationHandler) {
            callbacks.getOrPut(McpElicitations.Method) { mutableListOf() }.add(
                McpCallback(McpElicitations.Request.Params.Form::class) { request, requestId ->
                    if (requestId == null) return@McpCallback

                    val response = with(request) {
                        fn(
                            ElicitationRequest.Form(
                                message,
                                requestedSchema,
                                MetaKey.progressToken<Any>().toLens()(_meta),
                                task
                            )
                        )
                    }
                    http.send(
                        McpElicitations.Response(response.toProtocol(), requestId)
                    )
                }
            )
            callbacks.getOrPut(McpElicitations.Method) { mutableListOf() }.add(
                McpCallback(McpElicitations.Request.Params.Url::class) { request, requestId ->
                    if (requestId == null) return@McpCallback

                    val response = with(request) {
                        fn(
                            ElicitationRequest.Url(
                                message,
                                url,
                                elicitationId,
                                MetaKey.progressToken<Any>().toLens()(_meta),
                                task
                            )
                        )
                    }
                    http.send(
                        McpElicitations.Response(response.toProtocol(), requestId)
                    )
                }
            )
        }

        override fun onComplete(fn: (ElicitationId) -> Unit) {
            callbacks.getOrPut(McpElicitations.Complete.Method) { mutableListOf() }.add(
                McpCallback(McpElicitations.Complete.Notification.Params::class) { notification, _ ->
                    fn(notification.elicitationId)
                }
            )
        }
    }

    override fun sampling() = object : McpClient.Sampling {
        override fun onSampled(overrideDefaultTimeout: Duration?, fn: SamplingHandler) {
            callbacks.getOrPut(McpSampling.Method) { mutableListOf() }.add(
                McpCallback(McpSampling.Request.Params::class) { request, requestId ->
                    if (requestId == null) return@McpCallback

                    val responses =
                        with(request) {
                            fn(
                                SamplingRequest(
                                    messages,
                                    maxTokens,
                                    systemPrompt,
                                    includeContext,
                                    temperature,
                                    stopSequences,
                                    modelPreferences,
                                    metadata,
                                    tools ?: emptyList(),
                                    toolChoice,
                                    MetaKey.progressToken<Any>().toLens()(_meta)
                                )
                            )
                        }
                    responses.forEach { response ->
                        val protocolResponse = when (response) {
                            is SamplingResponse.Ok -> McpSampling.Response.Result(
                                response.model,
                                response.stopReason,
                                response.role,
                                response.content
                            )

                            is SamplingResponse.Task -> McpSampling.Response.Result(task = response.task)
                            is Error -> throw McpException(DomainError(response.message))
                        }
                        http.send(
                            McpSampling.Response(protocolResponse, requestId)
                        )
                    }
                })
        }
    }

    override fun resources() = object : McpClient.Resources {

        private val subscriptions = mutableMapOf<Uri, MutableList<() -> Unit>>()

        override fun onChange(fn: () -> Unit) {
            callbacks.getOrPut(McpResource.List.Changed.Method) { mutableListOf() }.add(
                McpCallback(McpResource.List.Changed.Notification.Params::class) { _, _ -> fn() }
            )
        }

        override fun list(overrideDefaultTimeout: Duration?) =
            http.send(McpResource.List.Request(McpResource.List.Request.Params(), nextId()))
                .flatMap { it.first().asAOrFailure<McpResource.List.Response.Result>() }
                .map { it.resources }

        override fun listTemplates(overrideDefaultTimeout: Duration?) =
            http.send(McpResource.ListTemplates.Request(McpResource.ListTemplates.Request.Params(), nextId()))
                .flatMap { it.first().asAOrFailure<McpResource.ListTemplates.Response.Result>() }
                .map { it.resourceTemplates }

        override fun read(
            request: ResourceRequest,
            overrideDefaultTimeout: Duration?
        ) = http.send(McpResource.Read.Request(McpResource.Read.Request.Params(request.uri), nextId()))
            .flatMap { it.first().asAOrFailure<McpResource.Read.Response.Result>() }
            .map { ResourceResponse.Ok(it.contents) as ResourceResponse }
            .flatMapFailure { toResourceErrorOrFailure(it) }

        override fun subscribe(uri: Uri, fn: () -> Unit) {
            callbacks.getOrPut(McpResource.Updated.Method) { mutableListOf() }.add(
                McpCallback(McpResource.Updated.Notification.Params::class) { notification, _ ->
                    subscriptions[notification.uri]?.forEach { it() }
                })
            http.send(McpResource.Subscribe.Request(McpResource.Subscribe.Request.Params(uri), nextId()))
            subscriptions.getOrPut(uri, ::mutableListOf).add(fn)
        }

        override fun unsubscribe(uri: Uri) {
            http.send(McpResource.Unsubscribe.Request(McpResource.Unsubscribe.Request.Params(uri), nextId()))
            subscriptions -= uri
        }
    }

    override fun completions() = object : McpClient.Completions {
        override fun complete(ref: Reference, request: CompletionRequest, overrideDefaultTimeout: Duration?) =
            http.send(McpCompletion.Request(McpCompletion.Request.Params(ref, request.argument), nextId()))
                .flatMap { it.first().asAOrFailure<McpCompletion.Response.Result>() }
                .map { it.completion.run { CompletionResponse.Ok(values, total, hasMore) as CompletionResponse } }
                .flatMapFailure { toCompletionErrorOrFailure(it) }
    }

    override fun tasks() = object : McpClient.Tasks {
        override fun onUpdate(fn: (org.http4k.ai.mcp.model.Task, Meta) -> Unit) {
            callbacks.getOrPut(McpTask.Status.Method) { mutableListOf() }.add(
                McpCallback(McpTask.Status.Notification.Params::class) { notification, _ ->
                    fn(
                        notification.toTask(),
                        notification._meta
                    )
                }
            )
        }

        override fun get(taskId: TaskId, overrideDefaultTimeout: Duration?) =
            http.send(McpTask.Get.Request(McpTask.Get.Request.Params(taskId), nextId()))
                .flatMap { it.first().asAOrFailure<McpTask.Get.Response.Result>() }
                .map { it.task }

        override fun list(overrideDefaultTimeout: Duration?) =
            http.send(McpTask.List.Request(McpTask.List.Request.Params(), nextId()))
                .flatMap { it.first().asAOrFailure<McpTask.List.Response.Result>() }
                .map { it.tasks }

        override fun cancel(taskId: TaskId, overrideDefaultTimeout: Duration?) =
            http.send(McpTask.Cancel.Request(McpTask.Cancel.Request.Params(taskId), nextId()))
                .flatMap { it.first().asAOrFailure<McpTask.Cancel.Response.Result>() }
                .map { }

        override fun result(taskId: TaskId, overrideDefaultTimeout: Duration?) =
            http.send(McpTask.Result.Request(McpTask.Result.Request.Params(taskId), nextId()))
                .flatMap { it.first().asAOrFailure<McpTask.Result.Response.ResponseResult>() }
                .map { it.result }

        override fun update(task: org.http4k.ai.mcp.model.Task, meta: Meta, overrideDefaultTimeout: Duration?) {
            http.send(McpTask.Status.Notification(McpTask.Status.Notification.Params(task, meta)))
        }
    }

    override fun close() {}

    private fun HttpHandler.send(
        message: McpJsonRpcMessage
    ): McpResult<Sequence<Event>> {
        val response = this(
            message.toHttpRequest(protocolVersion, baseUri)
                .accept(TEXT_EVENT_STREAM)
                .with(Header.MCP_SESSION_ID of sessionId)
        )

        return when {
            response.status.successful -> {
                _sessionId.set(Header.MCP_SESSION_ID(response))
                Success(response.body.stream.chunkedSseSequence().filterIsInstance<Event>())
            }

            else -> Failure(Http(response))
        }
    }
}

private fun ElicitationResponse.toProtocol() = when (this) {
    is Ok -> McpElicitations.Response.Result(action, content, _meta = _meta)
    is Task -> McpElicitations.Response.Result(content = McpJson.nullNode(), task = task)
    is ElicitationResponse.Error -> throw McpException(DomainError(message))
}
