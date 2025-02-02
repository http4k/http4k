package org.http4k.mcp.client

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.format.MoshiObject
import org.http4k.format.renderRequest
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.lens.accept
import org.http4k.lens.contentType
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.PromptRequest
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse.Error
import org.http4k.mcp.ToolResponse.Ok
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.model.ToolName
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ProtocolVersion
import org.http4k.mcp.protocol.ServerCapabilities
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.HasMethod
import org.http4k.mcp.protocol.messages.McpCompletion
import org.http4k.mcp.protocol.messages.McpInitialize
import org.http4k.mcp.protocol.messages.McpPrompt
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.protocol.messages.McpSampling
import org.http4k.mcp.protocol.messages.McpTool
import org.http4k.mcp.util.McpJson
import org.http4k.sse.SseMessage.Event
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class Http4kMcpClient(
    private val sseRequest: Request,
    private val clientInfo: VersionedMcpEntity,
    private val capabilities: ClientCapabilities,
    http: HttpHandler,
    private val protocolVersion: ProtocolVersion,
) : McpClient {
    private var running = false

    private val http = ClientFilters.SetHostFrom(sseRequest.uri).then(http)
    private val endpoint = AtomicReference<String>()

    private val requests = ConcurrentHashMap<RequestId, Pair<CountDownLatch, (Event) -> Boolean>>()
    private val messageQueues = ConcurrentHashMap<RequestId, LinkedBlockingQueue<Event>>()

    override fun start(): Result<ServerCapabilities> {
        val startLatch = CountDownLatch(1)
        startThread(startLatch)
        startLatch.await()

        return performRequest(McpInitialize, McpInitialize.Request(clientInfo, capabilities, protocolVersion))
            .mapCatching { (messageQueues[it]?.first() ?: error("No queue")).asAOrThrow<McpInitialize.Response>() }
            .mapCatching { response ->
                notify(McpInitialize.Initialized, McpInitialize.Initialized.Notification)
                    .map { response.capabilities }
                    .getOrThrow()
            }
            .onFailure { close() }
    }

    override fun tools() = object : McpClient.Tools {
        override fun list() = performRequest(McpTool.List, McpTool.List.Request())
            .mapCatching { (messageQueues[it]?.first() ?: error("No queue")).asAOrThrow<McpTool.List.Response>() }
            .map { it.tools }

        override fun call(name: ToolName, request: ToolRequest) =
            performRequest(
                McpTool.Call,
                McpTool.Call.Request(name, request.mapValues { McpJson.asJsonObject(it.value) })
            )
                .mapCatching { (messageQueues[it]?.first() ?: error("No queue")).asAOrThrow<McpTool.Call.Response>() }
                .mapCatching {
                    when (it.isError) {
                        true -> Error(ErrorMessage(-1, it.content.joinToString()))
                        else -> Ok(it.content)
                    }
                }
    }

    override fun prompts() = object : McpClient.Prompts {
        override fun list() = performRequest(McpPrompt.List, McpPrompt.List.Request())
            .mapCatching { (messageQueues[it]?.first() ?: error("No queue")).asAOrThrow<McpPrompt.List.Response>() }
            .map { it.prompts }

        override fun get(name: String, request: PromptRequest) =
            performRequest(McpPrompt.Get, McpPrompt.Get.Request(name, request))
                .mapCatching { (messageQueues[it]?.first() ?: error("No queue")).asAOrThrow<McpPrompt.Get.Response>() }
                .map { PromptResponse(it.messages, it.description) }
    }

    override fun sampling() = object : McpClient.Sampling {
        override fun sample(name: ModelIdentifier, request: SamplingRequest): Sequence<Result<SamplingResponse>> {
            fun hasStopReason(message: Event): Boolean = message.data.contains(""""stopReason":"""")

            val messages = performRequest(
                McpSampling,
                with(request) {
                    McpSampling.Request(
                        messages,
                        maxTokens,
                        systemPrompt,
                        includeContext,
                        temperature,
                        stopSequences,
                        modelPreferences,
                        metadata
                    )
                },
                ::hasStopReason
            ).map { messageQueues[it] ?: error("No queue") }.getOrThrow()

            return sequence {
                while (true) {
                    val message = messages.take()
                    yield(
                        runCatching { message.asAOrThrow<McpSampling.Response>() }
                            .map { it: McpSampling.Response ->
                                SamplingResponse(
                                    it.model,
                                    it.stopReason,
                                    it.role,
                                    it.content
                                )
                            }
                    )

                    if (hasStopReason(message)) {
                        break
                    }
                }
            }
        }
    }

    override fun resources() = object : McpClient.Resources {
        override fun list() = performRequest(McpResource.List, McpResource.List.Request())
            .mapCatching {
                (messageQueues[it]?.first() ?: error("No queue")).asAOrThrow<McpResource.List.Response>()
            }
            .map { it.resources }

        override fun read(name: String, request: ResourceRequest) =
            performRequest(McpResource.Read, McpResource.Read.Request(request.uri))
                .mapCatching {
                    (messageQueues[it]?.first() ?: error("No queue")).asAOrThrow<McpResource.Read.Response>()
                }
                .map { ResourceResponse(it.contents) }
    }

    override fun completions() = object : McpClient.Completions {
        override fun complete(request: CompletionRequest) =
            performRequest(McpCompletion, McpCompletion.Request(request.ref, request.argument))
                .mapCatching {
                    (messageQueues[it]?.first() ?: error("No queue")).asAOrThrow<McpCompletion.Response>()
                }
                .map { CompletionResponse(it.completion) }
    }

    private fun notify(method: HasMethod, mcp: ClientMessage.Notification): Result<Unit> {
        val request = Request(POST, Uri.of(endpoint.get()))
            .contentType(APPLICATION_JSON)
            .body(
                McpJson.compact(
                    McpJson.renderRequest(
                        method.Method.value,
                        McpJson.asJsonObject(mcp),
                        McpJson.nullNode()
                    )
                )
            )

        return when {
            http(request).status.successful -> runCatching { Unit }
            else -> runCatching { error("Failed!") }
        }
    }

    private fun performRequest(
        method: HasMethod,
        request: ClientMessage.Request,
        isComplete: (Event) -> Boolean = { true }
    ): Result<RequestId> {
        val requestId = RequestId.random()

        val httpReq = Request(POST, Uri.of(endpoint.get()))
            .contentType(APPLICATION_JSON)
            .body(with(McpJson) {
                compact(renderRequest(method.Method.value, asJsonObject(request), asJsonObject(requestId)))
            })

        val latch = CountDownLatch(1)

        requests[requestId] = latch to isComplete
        messageQueues[requestId] = LinkedBlockingQueue<Event>()

        return when {
            http(httpReq).status.successful -> {
                latch.await()
                runCatching { requestId }
            }

            else -> {
                requests.remove(requestId)
                messageQueues.remove(requestId)
                runCatching { error("Failed!") }
            }
        }
    }

    private fun startThread(startLatch: CountDownLatch) = thread {
        do {
            try {
                val response = http(sseRequest.accept(TEXT_EVENT_STREAM))

                response.body.stream.chunkedSseSequence().forEach { msg ->
                    when (msg) {
                        is Event -> when (msg.event) {
                            "endpoint" -> {
                                endpoint.set(msg.data)
                                running = true
                                startLatch.countDown()
                            }

                            "ping" -> {}
                            else -> with(McpJson) {
                                val request = JsonRpcResult(this, (parse(msg.data) as MoshiObject).attributes)
                                val id = asA<RequestId>(compact(request.id ?: nullNode()))

                                messageQueues[id]?.let { queue ->
                                    queue.put(msg)

                                    val (latch, isComplete) = requests[id] ?: return@let
                                    if (isComplete(msg)) {
                                        requests.remove(id)
                                    }
                                    latch.countDown()
                                }
                            }
                        }

                        else -> {}
                    }
                    if (!running) return@thread
                }
            } catch (e: Exception) {
                System.err.println("Error: $e")
                e.printStackTrace()
            }

        } while (running)
    }

    override fun close() {
        running = false
    }
}

private inline fun <reified T : Any> Event.asAOrThrow() = with(McpJson) {
    val result = JsonRpcResult(this, (parse(data) as MoshiObject).attributes)
    when {
        result.isError() -> error("Failed: " + asFormatString(result.error ?: nullNode()))
        else -> asA<T>(compact(result.result ?: nullNode()))
    }
}
