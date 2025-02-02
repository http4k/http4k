package org.http4k.mcp.client

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.format.MoshiObject
import org.http4k.format.renderRequest
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.lens.contentType
import org.http4k.mcp.client.McpClient.Completions
import org.http4k.mcp.client.McpClient.Prompts
import org.http4k.mcp.client.McpClient.Resources
import org.http4k.mcp.client.McpClient.Sampling
import org.http4k.mcp.client.McpClient.Tools
import org.http4k.mcp.client.internal.ClientCompletions
import org.http4k.mcp.client.internal.ClientPrompts
import org.http4k.mcp.client.internal.ClientResources
import org.http4k.mcp.client.internal.ClientSampling
import org.http4k.mcp.client.internal.ClientTools
import org.http4k.mcp.client.internal.NotificationCallback
import org.http4k.mcp.client.internal.asAOrThrow
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ProtocolVersion
import org.http4k.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.mcp.protocol.ServerCapabilities
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.McpInitialize
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.util.McpJson
import org.http4k.sse.Http4kSseClient
import org.http4k.sse.SseMessage.Event
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Single connection MCP client.
 */
class Http4kMcpClient(
    private val sseRequest: Request,
    private val clientInfo: VersionedMcpEntity,
    private val capabilities: ClientCapabilities,
    http: HttpHandler,
    private val protocolVersion: ProtocolVersion = LATEST_VERSION,
) : McpClient {
    private val running = AtomicBoolean(false)

    private val http = ClientFilters.SetHostFrom(sseRequest.uri).then(http)

    private val sseClient = Http4kSseClient(http)
    private val endpoint = AtomicReference<String>()

    private val requests = ConcurrentHashMap<RequestId, Pair<CountDownLatch, (Event) -> Boolean>>()

    private val notificationCallbacks = mutableListOf<NotificationCallback<*>>()

    private val messageQueues = ConcurrentHashMap<RequestId, LinkedBlockingQueue<Event>>()

    override fun start(): Result<ServerCapabilities> {
        val startLatch = CountDownLatch(1)
        sseClient(sseRequest) {
            when (it) {
                is Event -> when (it.event) {
                    "endpoint" -> {
                        endpoint.set(it.data)
                        running.set(true)
                        startLatch.countDown()
                    }

                    "ping" -> {}
                    else -> with(McpJson) {
                        val request = JsonRpcResult(this, (parse(it.data) as MoshiObject).attributes)
                        val id = asA<RequestId>(compact(request.id ?: nullNode()))

                        messageQueues[id]
                            ?.also { queue ->
                                queue.put(it)

                                val (latch, isComplete) = requests[id] ?: return@also
                                if (isComplete(it)) requests.remove(id)
                                latch.countDown()
                            } // TODO add notifications here
                    }
                }

                else -> {}
            }
            running.get()
        }

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

    override fun tools(): Tools =
        ClientTools(::findQueue, ::performRequest, notificationCallbacks::add)

    override fun prompts(): Prompts =
        ClientPrompts(::findQueue, ::performRequest, notificationCallbacks::add)

    override fun sampling(): Sampling =
        ClientSampling(::findQueue, ::performRequest)

    override fun resources(): Resources =
        ClientResources(::findQueue, ::performRequest, notificationCallbacks::add)

    override fun completions(): Completions =
        ClientCompletions(::findQueue, ::performRequest)

    private fun notify(method: McpRpc, mcp: ClientMessage.Notification): Result<Unit> {
        val response = http(toHttpRequest(method, mcp))
        return when {
            response.status.successful -> runCatching { Unit }
            else -> runCatching { error("Failed HTTP ${response.status}") }
        }
    }

    private fun findQueue(id: RequestId): LinkedBlockingQueue<Event> =
        messageQueues[id] ?: error("no queue")

    private fun performRequest(
        method: McpRpc,
        request: ClientMessage,
        isComplete: (Event) -> Boolean = { true }
    ): Result<RequestId> {
        val requestId = RequestId.random()

        val latch = CountDownLatch(1)

        requests[requestId] = latch to isComplete
        messageQueues[requestId] = LinkedBlockingQueue<Event>()

        val response = http(toHttpRequest(method, request, requestId))
        return when {
            response.status.successful -> {
                latch.await()
                runCatching { requestId }
            }

            else -> {
                latch.await()
                requests.remove(requestId)
                messageQueues.remove(requestId)
                runCatching { error("Failed HTTP ${response.status}") }
            }
        }
    }

    private fun toHttpRequest(
        rpc: McpRpc,
        request: ClientMessage,
        requestId: RequestId? = null
    ) = Request(POST, Uri.of(endpoint.get()))
        .contentType(APPLICATION_JSON)
        .body(with(McpJson) {
            compact(
                renderRequest(rpc.Method.value,
                    asJsonObject(request),
                    requestId?.let { asJsonObject(it) } ?: nullNode())
            )
        })

    override fun close() {
        running.set(false)
        sseClient.close()
    }
}
