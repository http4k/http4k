package org.http4k.mcp.client

import org.http4k.core.Request
import org.http4k.format.MoshiObject
import org.http4k.format.renderRequest
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
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
import org.http4k.mcp.protocol.McpException
import org.http4k.mcp.protocol.McpRpcMethod
import org.http4k.mcp.protocol.ProtocolVersion
import org.http4k.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.mcp.protocol.ServerCapabilities
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.McpInitialize
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpJson.asJsonObject
import org.http4k.mcp.util.McpJson.compact
import org.http4k.mcp.util.McpNodeType
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Event
import org.http4k.websocket.WebsocketFactory
import org.http4k.websocket.WsMessage
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Single connection MCP client.
 */
class WsMcpClient(
    private val wsRequest: Request,
    private val websocketFactory: WebsocketFactory,
    private val clientInfo: VersionedMcpEntity,
    private val capabilities: ClientCapabilities,
    private val protocolVersion: ProtocolVersion = LATEST_VERSION,
) : McpClient {
    private val running = AtomicBoolean(false)

    private val wsClient by lazy { websocketFactory.blocking(wsRequest.uri, wsRequest.headers) }
    private val requests = ConcurrentHashMap<RequestId, Pair<CountDownLatch, (McpNodeType) -> Boolean>>()

    private val notificationCallbacks = mutableMapOf<McpRpcMethod, MutableList<NotificationCallback<*>>>()

    private val messageQueues = ConcurrentHashMap<RequestId, BlockingQueue<McpNodeType>>()

    override fun start(): Result<ServerCapabilities> {
        val startLatch = CountDownLatch(1)

        thread {
            wsClient
                .received()
                .forEach {
                    when (val msg = SseMessage.parse(it.bodyString())) {
                        is Event -> when (msg.event) {
                            "endpoint" -> startLatch.countDown()

                            "ping" -> {}
                            else -> with(McpJson) {
                                val data = parse(msg.data) as MoshiObject

                                when {
                                    data["id"] == null -> {
                                        val message = JsonRpcRequest(this, data.attributes)
                                        notificationCallbacks[McpRpcMethod.of(message.method)]?.forEach {
                                            it.process(message)
                                        }
                                    }

                                    else -> {
                                        val message = JsonRpcResult(this, data.attributes)
                                        val id = asA<RequestId>(compact(message.id ?: nullNode()))
                                        messageQueues[id]?.put(data)
                                        val (latch, isComplete) = requests[id] ?: return@forEach
                                        if (message.isError() || isComplete(data)) {
                                            requests.remove(id)
                                        }
                                        latch.countDown()
                                    }
                                }
                            }
                        }

                        else -> {}
                    }
                    running.get()
                }
        }

        startLatch.await()

        return performRequest(McpInitialize, McpInitialize.Request(clientInfo, capabilities, protocolVersion))
            .mapCatching { reqId: RequestId ->
                (messageQueues[reqId]?.take()
                    ?: throw McpException(InternalError)).asAOrThrow<McpInitialize.Response>()
                    .also { messageQueues.remove(reqId) }
            }
            .mapCatching { response ->
                notify(McpInitialize.Initialized, McpInitialize.Initialized.Notification)
                    .map { response.capabilities }
                    .getOrThrow()
            }
            .onFailure { close() }
    }

    override fun tools(): Tools =
        ClientTools(::findQueue, ::performRequest) { rpc, callback ->
            notificationCallbacks.getOrPut(rpc.Method) { mutableListOf() }.add(callback)
        }

    override fun prompts(): Prompts =
        ClientPrompts(::findQueue, ::performRequest) { rpc, callback ->
            notificationCallbacks.getOrPut(rpc.Method) { mutableListOf() }.add(callback)
        }

    override fun sampling(): Sampling =
        ClientSampling(::findQueue, ::performRequest)

    override fun resources(): Resources =
        ClientResources(::findQueue, ::performRequest) { rpc, callback ->
            notificationCallbacks.getOrPut(rpc.Method) { mutableListOf() }.add(callback)
        }

    override fun completions(): Completions =
        ClientCompletions(::findQueue, ::performRequest)

    private fun notify(rpc: McpRpc, mcp: ClientMessage.Notification): Result<Unit> {
        wsClient.send(
            WsMessage(
                compact(
                    McpJson.renderRequest(
                        rpc.Method.value,
                        asJsonObject(mcp),
                        McpJson.nullNode()
                    )
                )
            )
        )
        return Result.success(Unit)
    }

    private fun findQueue(id: RequestId) = messageQueues[id] ?: error("no queue")

    private fun performRequest(rpc: McpRpc, request: ClientMessage, isComplete: (McpNodeType) -> Boolean = { true })
        : Result<RequestId> {
        val requestId = RequestId.random()

        val latch = CountDownLatch(if (request is ClientMessage.Notification) 0 else 1)

        requests[requestId] = latch to isComplete
        messageQueues[requestId] = LinkedBlockingQueue()

        wsClient.send(
            WsMessage(
                compact(
                    McpJson.renderRequest(
                        rpc.Method.value,
                        asJsonObject(request),
                        asJsonObject(requestId)
                    )
                )
            )
        )
        latch.await()

        return Result.success(requestId)
    }

    override fun close() {
        running.set(false)
        wsClient.close()
    }
}
