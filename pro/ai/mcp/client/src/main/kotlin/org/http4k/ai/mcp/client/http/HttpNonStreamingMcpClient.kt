/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.http

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.flatMapFailure
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.resultFrom
import org.http4k.ai.mcp.CompletionRequest
import org.http4k.ai.mcp.CompletionResponse
import org.http4k.ai.mcp.McpError.Http
import org.http4k.ai.mcp.McpError.Protocol
import org.http4k.ai.mcp.McpResult
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.client.asAOrFailure
import org.http4k.ai.mcp.client.internal.toCompletionErrorOrFailure
import org.http4k.ai.mcp.client.internal.toPromptErrorOrFailure
import org.http4k.ai.mcp.client.internal.toResourceErrorOrFailure
import org.http4k.ai.mcp.client.internal.toToolElicitationRequiredOrError
import org.http4k.ai.mcp.client.internal.toToolResponseOrError
import org.http4k.ai.mcp.client.toHttpRequest
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Progress
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.ClientProtocolCapability
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.protocol.messages.McpInitialize
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpTask
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.model.ToolName
import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.MCP_SESSION_ID
import org.http4k.lens.accept
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Event
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * JSON Rpc connection MCP client.
 */
class HttpNonStreamingMcpClient(
    private val baseUri: Uri,
    private val entity: McpEntity = McpEntity.of("http4k-mcp-client"),
    private val version: Version = Version.of("0.0.0"),
    private val http: HttpHandler = JavaHttpClient(),
    private val protocolVersion: ProtocolVersion = LATEST_VERSION,
) : McpClient {

    private val _sessionId = AtomicReference<SessionId>()

    private val id = AtomicLong(0)

    override val sessionId get() = _sessionId.get()

    private fun nextId() = McpMessageId.of(id.incrementAndGet())

    override fun start(overrideDefaultTimeout: Duration?) =
        http.send<McpInitialize.Response.Result>(
            McpInitialize.Request(
                McpInitialize.Request.Params(
                    VersionedMcpEntity(entity, version),
                    ClientCapabilities(*listOf<ClientProtocolCapability>().toTypedArray()),
                    protocolVersion
                ), nextId()
            )
        )

    override fun progress() = object : McpClient.RequestProgress {
        override fun onProgress(fn: (Progress) -> Unit) =
            throw UnsupportedOperationException()
    }

    override fun tools() = object : McpClient.Tools {

        private var lastKnownTools = emptyList<McpTool>()

        override fun onChange(fn: () -> Unit) = throw UnsupportedOperationException()

        override fun list(overrideDefaultTimeout: Duration?) =
            http.send<McpTool.List.Response.Result>(McpTool.List.Request(McpTool.List.Request.Params(), nextId()))
                .map { it.tools.also { lastKnownTools = it } }

        override fun call(
            name: ToolName,
            request: ToolRequest,
            overrideDefaultTimeout: Duration?
        ) = McpTool.Call.Request(
            McpTool.Call.Request.Params(name, request.mapValues { McpJson.asJsonObject(it.value) }),
            nextId()
        ).let { mcpRequest ->
            PopulateToolHeaders(lastKnownTools, mcpRequest.method, name, request).then(http).send<McpTool.Call.Response.Result>(mcpRequest)
                .map { toToolResponseOrError(it) }
                .flatMapFailure { toToolElicitationRequiredOrError(it) }
        }
    }

    override fun prompts() = object : McpClient.Prompts {
        override fun onChange(fn: () -> Unit) = throw UnsupportedOperationException()

        override fun list(overrideDefaultTimeout: Duration?) =
            http.send<McpPrompt.List.Response.Result>(McpPrompt.List.Request(McpPrompt.List.Request.Params(), nextId()))
                .map { it.prompts }

        override fun get(
            name: PromptName,
            request: PromptRequest,
            overrideDefaultTimeout: Duration?
        ) = McpPrompt.Get.Request(McpPrompt.Get.Request.Params(name, request), nextId()).let { mcpRequest ->
            PopulateMcpHeaders(mcpRequest.method, name.value).then(http).send<McpPrompt.Get.Response.Result>(mcpRequest)
                .map { PromptResponse.Ok(it.messages, it.description) as PromptResponse }
                .flatMapFailure { toPromptErrorOrFailure(it) }
        }
    }

    override fun sampling() = throw UnsupportedOperationException()

    override fun elicitations(): McpClient.Elicitations = throw UnsupportedOperationException()

    override fun resources() = object : McpClient.Resources {
        override fun onChange(fn: () -> Unit) = throw UnsupportedOperationException()

        override fun list(overrideDefaultTimeout: Duration?) =
            http.send<McpResource.List.Response.Result>(McpResource.List.Request(McpResource.List.Request.Params(), nextId()))
                .map { it.resources }

        override fun listTemplates(overrideDefaultTimeout: Duration?) =
            http.send<McpResource.ListTemplates.Response.Result>(
                McpResource.ListTemplates.Request(McpResource.ListTemplates.Request.Params(), nextId())
            )
                .map { it.resourceTemplates }

        override fun read(
            request: ResourceRequest,
            overrideDefaultTimeout: Duration?
        ) = McpResource.Read.Request(McpResource.Read.Request.Params(request.uri), nextId()).let { mcpRequest ->
            PopulateMcpHeaders(mcpRequest.method, request.uri.toString()).then(http).send<McpResource.Read.Response.Result>(mcpRequest)
                .map { ResourceResponse.Ok(it.contents) as ResourceResponse }
                .flatMapFailure { toResourceErrorOrFailure(it) }
        }

        override fun subscribe(uri: Uri, fn: () -> Unit) = throw UnsupportedOperationException()

        override fun unsubscribe(uri: Uri) = throw UnsupportedOperationException()
    }

    override fun completions() = object : McpClient.Completions {
        override fun complete(ref: Reference, request: CompletionRequest, overrideDefaultTimeout: Duration?) =
            http.send<McpCompletion.Response.Result>(McpCompletion.Request(McpCompletion.Request.Params(ref, request.argument), nextId()))
                .map { it.completion.run { CompletionResponse.Ok(values, total, hasMore) as CompletionResponse } }
                .flatMapFailure { toCompletionErrorOrFailure(it) }
    }

    override fun tasks() = object : McpClient.Tasks {
        override fun onUpdate(fn: (Task, Meta) -> Unit) = throw UnsupportedOperationException()

        override fun get(taskId: TaskId, overrideDefaultTimeout: Duration?) =
            http.send<McpTask.Get.Response.Result>(McpTask.Get.Request(McpTask.Get.Request.Params(taskId), nextId()))
                .map { it.task }

        override fun list(overrideDefaultTimeout: Duration?) =
            http.send<McpTask.List.Response.Result>(McpTask.List.Request(McpTask.List.Request.Params(), nextId()))
                .map { it.tasks }

        override fun cancel(taskId: TaskId, overrideDefaultTimeout: Duration?) =
            http.send<McpTask.Cancel.Response.Result>(McpTask.Cancel.Request(McpTask.Cancel.Request.Params(taskId), nextId()))
                .map { }

        override fun result(taskId: TaskId, overrideDefaultTimeout: Duration?) =
            http.send<McpTask.Result.Response.ResponseResult>(McpTask.Result.Request(McpTask.Result.Request.Params(taskId), nextId()))
                .map { it.result }

        override fun update(task: Task, meta: Meta, overrideDefaultTimeout: Duration?) =
            throw UnsupportedOperationException()
    }

    override fun close() {}

    private inline fun <reified T : Any> HttpHandler.send(message: McpJsonRpcRequest): McpResult<T> {
        val response = this(
            message.toHttpRequest(protocolVersion, baseUri)
                .with(Header.MCP_SESSION_ID of sessionId)
                .accept(TEXT_EVENT_STREAM)
        )

        val newSessionId = Header.MCP_SESSION_ID(response)

        if(newSessionId != sessionId) {
            _sessionId.set(newSessionId)
        }

        return when {
            response.status.successful -> resultFrom { SseMessage.parse(response.bodyString()) as Event }
                .flatMap { it.asAOrFailure<T>() }
                .flatMapFailure {
                    when (it) {
                        is Protocol -> Failure(it)
                        else -> Failure(Http(response))
                    }
                }

            else -> Failure(Http(response))
        }
    }
}
