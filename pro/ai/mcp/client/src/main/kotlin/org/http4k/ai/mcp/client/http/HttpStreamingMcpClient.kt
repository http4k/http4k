package org.http4k.ai.mcp.client.http

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.resultFrom
import org.http4k.ai.mcp.CompletionRequest
import org.http4k.ai.mcp.CompletionResponse
import org.http4k.ai.mcp.ElicitationHandler
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.McpError
import org.http4k.ai.mcp.McpError.Http
import org.http4k.ai.mcp.McpResult
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.SamplingHandler
import org.http4k.ai.mcp.SamplingRequest
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.client.asAOrFailure
import org.http4k.ai.mcp.client.internal.McpCallback
import org.http4k.ai.mcp.client.toHttpRequest
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.model.Progress
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.ClientCapabilities.Companion.All
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.ServerCapabilities
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.protocol.messages.ClientMessage
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.protocol.messages.McpElicitations
import org.http4k.ai.mcp.protocol.messages.McpInitialize
import org.http4k.ai.mcp.protocol.messages.McpProgress
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpRpc
import org.http4k.ai.mcp.protocol.messages.McpSampling
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.asA
import org.http4k.ai.mcp.util.McpJson.compact
import org.http4k.ai.model.ToolName
import org.http4k.client.Http4kSseClient
import org.http4k.client.JavaHttpClient
import org.http4k.client.ReconnectionMode
import org.http4k.client.ReconnectionMode.Immediate
import org.http4k.sse.chunkedSseSequence
import org.http4k.core.BodyMode.Stream
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.lens.Header
import org.http4k.lens.MCP_PROTOCOL_VERSION
import org.http4k.lens.MCP_SESSION_ID
import org.http4k.lens.accept
import org.http4k.sse.SseMessage.Event
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicReference
import kotlin.Long.Companion.MAX_VALUE
import kotlin.concurrent.thread

/**
 * HTTP Streaming connection MCP client
 */
class HttpStreamingMcpClient(
    private val name: McpEntity,
    private val version: Version,
    private val baseUri: Uri,
    private val http: HttpHandler = JavaHttpClient(responseBodyMode = Stream),
    private val capabilities: ClientCapabilities = All,
    private val protocolVersion: ProtocolVersion = LATEST_VERSION,
    private val notificationSseReconnectionMode: ReconnectionMode = Immediate
) : McpClient {
    private val callbacks = mutableMapOf<McpRpcMethod, MutableList<McpCallback<*>>>()

    private val sessionId = AtomicReference<SessionId>()

    override fun start(overrideDefaultTimeout: Duration?): Result<ServerCapabilities, McpError> = http.send(
        McpInitialize, McpInitialize.Request(
            VersionedMcpEntity(name, version),
            capabilities,
            protocolVersion
        )
    )
        .flatMap { it.first().asAOrFailure<McpInitialize.Response>() }
        .map(McpInitialize.Response::capabilities)
        .also {
            val latch = CountDownLatch(1)
            thread(isDaemon = true) {
                Http4kSseClient(
                    Request(GET, baseUri)
                        .with(Header.MCP_SESSION_ID of sessionId.get())
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
                    .forEach {
                        val message = JsonRpcRequest(McpJson, it.attributes)
                        val id = message.id?.let { asA<McpMessageId>(compact(it)) }
                        callbacks[McpRpcMethod.of(message.method)]?.any { it(message, id) }
                    }
            }

            latch.await(overrideDefaultTimeout?.toMillis() ?: MAX_VALUE, MILLISECONDS)
        }


    override fun progress() = object : McpClient.RequestProgress {
        override fun onProgress(fn: (Progress) -> Unit) {
            callbacks.getOrPut(McpProgress.Method) { mutableListOf() }.add(
                McpCallback(McpProgress.Notification::class) { n, _ ->
                    fn(Progress(n.progressToken, n.progress, n.total, n.description))
                }
            )
        }
    }

    override fun tools() = object : McpClient.Tools {
        override fun onChange(fn: () -> Unit) {
            callbacks.getOrPut(McpTool.List.Changed.Method) { mutableListOf() }.add(
                McpCallback(McpPrompt.List.Changed.Notification::class) { _, _ -> fn() }
            )
        }

        override fun list(overrideDefaultTimeout: Duration?) =
            http.send(McpTool.List, McpTool.List.Request())
                .flatMap { it.first().asAOrFailure<McpTool.List.Response>() }
                .map { it.tools }

        override fun call(
            name: ToolName,
            request: ToolRequest,
            overrideDefaultTimeout: Duration?
        ): Result<ToolResponse, McpError> {
            val incoming = http.send(
                McpTool.Call,
                McpTool.Call.Request(
                    name,
                    request.mapValues { McpJson.asJsonObject(it.value) },
                    request.meta
                )
            )
            return incoming
                .flatMap {
                    it.mapNotNull {
                        when ((McpJson.parse(it.data) as MoshiObject)["method"]) {
                            null -> it
                            else -> {
                                val message =
                                    JsonRpcRequest(McpJson, (McpJson.parse(it.data) as MoshiObject).attributes)
                                val id = message.id?.let { asA<McpMessageId>(compact(it)) }
                                callbacks[McpRpcMethod.of(message.method)]?.any { it(message, id) }
                                null
                            }
                        }
                    }.first().asAOrFailure<McpTool.Call.Response>()
                }
                .map {
                    when (it.isError) {
                        true -> Error(
                            ErrorMessage(
                                -1, it.content?.joinToString()
                                    ?: it.structuredContent?.let { McpJson.asFormatString(it) }
                                    ?: "<no message"
                            )
                        )

                        else -> Ok(
                            it.content,
                            it.structuredContent?.let(McpJson::convert),
                            it._meta
                        )
                    }
                }
        }
    }

    override fun prompts() = object : McpClient.Prompts {
        override fun onChange(fn: () -> Unit) {
            callbacks.getOrPut(McpPrompt.List.Changed.Method) { mutableListOf() }.add(
                McpCallback(McpPrompt.List.Changed.Notification::class) { _, _ -> fn() }
            )
        }

        override fun list(overrideDefaultTimeout: Duration?) =
            http.send(McpPrompt.List, McpPrompt.List.Request())
                .flatMap { it.first().asAOrFailure<McpPrompt.List.Response>() }
                .map { it.prompts }

        override fun get(
            name: PromptName,
            request: PromptRequest,
            overrideDefaultTimeout: Duration?
        ) = http.send(McpPrompt.Get, McpPrompt.Get.Request(name, request))
            .flatMap { it.first().asAOrFailure<McpPrompt.Get.Response>() }
            .map { PromptResponse(it.messages, it.description) }
    }

    override fun elicitations() = object : McpClient.Elicitations {
        override fun onElicitation(overrideDefaultTimeout: Duration?, fn: ElicitationHandler) {
            callbacks.getOrPut(McpElicitations.Method) { mutableListOf() }.add(
                McpCallback(McpElicitations.Request.Form::class) { request, requestId ->
                    if (requestId == null) return@McpCallback

                    with(with(request) { fn(ElicitationRequest.Form(message, requestedSchema, _meta.progressToken)) }) {
                        http.send(
                            McpElicitations,
                            McpElicitations.Response(action, content, _meta),
                            requestId
                        )
                    }
                }
            )
            callbacks.getOrPut(McpElicitations.Method) { mutableListOf() }.add(
                McpCallback(McpElicitations.Request.Url::class) { request, requestId ->
                    if (requestId == null) return@McpCallback

                    with(with(request) {
                        fn(
                            ElicitationRequest.Url(
                                message,
                                url,
                                elicitationId,
                                _meta.progressToken
                            )
                        )
                    }) {
                        http.send(
                            McpElicitations,
                            McpElicitations.Response(action, content, _meta),
                            requestId
                        )
                    }
                }
            )
        }
    }

    override fun sampling() = object : McpClient.Sampling {
        override fun onSampled(overrideDefaultTimeout: Duration?, fn: SamplingHandler) {
            callbacks.getOrPut(McpSampling.Method) { mutableListOf() }.add(
                McpCallback(McpSampling.Request::class) { request, requestId ->
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
                                    _meta.progressToken
                                )
                            )
                        }
                    responses.forEach {
                        http.send(
                            McpSampling,
                            McpSampling.Response(it.model, it.stopReason, it.role, it.content),
                            requestId
                        )
                    }
                })
        }
    }

    override fun resources() = object : McpClient.Resources {

        private val subscriptions = mutableMapOf<Uri, MutableList<() -> Unit>>()

        override fun onChange(fn: () -> Unit) {
            callbacks.getOrPut(McpResource.List.Changed.Method) { mutableListOf() }.add(
                McpCallback(McpResource.List.Changed.Notification::class) { _, _ -> fn() }
            )
        }

        override fun list(overrideDefaultTimeout: Duration?) =
            http.send(McpResource.List, McpResource.List.Request())
                .flatMap { it.first().asAOrFailure<McpResource.List.Response>() }
                .map { it.resources }

        override fun listTemplates(overrideDefaultTimeout: Duration?) =
            http.send(McpResource.ListTemplates, McpResource.ListTemplates.Request())
                .flatMap { it.first().asAOrFailure<McpResource.ListTemplates.Response>() }
                .map { it.resourceTemplates }

        override fun read(
            request: ResourceRequest,
            overrideDefaultTimeout: Duration?
        ) = http.send(McpResource.Read, McpResource.Read.Request(request.uri))
            .flatMap { it.first().asAOrFailure<McpResource.Read.Response>() }
            .map { ResourceResponse(it.contents) }

        override fun subscribe(uri: Uri, fn: () -> Unit) {
            callbacks.getOrPut(McpResource.Updated.Method) { mutableListOf() }.add(
                McpCallback(McpPrompt.List.Changed.Notification::class) { _, _ ->
                    subscriptions[uri]?.forEach { it() }
                })
            subscriptions.getOrPut(uri, ::mutableListOf).add(fn)
        }

        override fun unsubscribe(uri: Uri) {
            subscriptions -= uri
        }
    }

    override fun completions() = object : McpClient.Completions {
        override fun complete(ref: Reference, request: CompletionRequest, overrideDefaultTimeout: Duration?) =
            http.send(McpCompletion, McpCompletion.Request(ref, request.argument))
                .flatMap { it.first().asAOrFailure<McpCompletion.Response>() }
                .map { it.completion.run { CompletionResponse(values, total, hasMore) } }
    }

    override fun close() {}

    private fun HttpHandler.send(
        rpc: McpRpc,
        message: ClientMessage,
        messageId: McpMessageId? = null
    ): McpResult<Sequence<Event>> {
        val response = this(
            message.toHttpRequest(protocolVersion, baseUri, rpc, messageId)
                .accept(TEXT_EVENT_STREAM)
                .with(Header.MCP_SESSION_ID of sessionId.get())
        )

        return when {
            response.status.successful -> {
                sessionId.set(Header.MCP_SESSION_ID(response))

                Success(response.body.stream.chunkedSseSequence().filterIsInstance<Event>())
            }

            else -> Failure(Http(response))
        }
    }
}
