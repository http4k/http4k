package org.http4k.mcp.client

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.mcp.client.internal.ClientCompletions
import org.http4k.mcp.client.internal.ClientPrompts
import org.http4k.mcp.client.internal.ClientResources
import org.http4k.mcp.client.internal.ClientSampling
import org.http4k.mcp.client.internal.ClientTools
import org.http4k.mcp.client.internal.NotificationCallback
import org.http4k.mcp.client.internal.asOrFailure
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.McpRpcMethod
import org.http4k.mcp.protocol.ProtocolVersion
import org.http4k.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.mcp.protocol.ServerCapabilities
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.McpInitialize
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Event
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

abstract class AbstractMcpClient(
    private val clientInfo: VersionedMcpEntity,
    private val capabilities: ClientCapabilities,
    private val protocolVersion: ProtocolVersion = LATEST_VERSION,
) : McpClient {
    private val running = AtomicBoolean(false)
    protected val requests = ConcurrentHashMap<RequestId, CountDownLatch>()
    private val notificationCallbacks = mutableMapOf<McpRpcMethod, MutableList<NotificationCallback<*>>>()
    protected val messageQueues = ConcurrentHashMap<RequestId, BlockingQueue<McpNodeType>>()

    override fun start(): McpResult<ServerCapabilities> {
        val startLatch = CountDownLatch(1)

        thread(isDaemon = true) {
            received().forEach {
                when (it) {
                    is Event -> when (it.event) {
                        "endpoint" -> {
                            endpoint(it)
                            running.set(true)
                            startLatch.countDown()
                        }

                        "ping" -> {}
                        else -> {
                            with(McpJson) {
                                val data = parse(it.data) as MoshiObject

                                when {
                                    data["id"] == null -> {
                                        val message = JsonRpcRequest(this, data.attributes)
                                        notificationCallbacks[McpRpcMethod.of(message.method)]?.forEach { it(message) }
                                    }

                                    else -> {
                                        val message = JsonRpcResult(this, data.attributes)
                                        val id = asA<RequestId>(compact(message.id ?: nullNode()))
                                        messageQueues[id]?.add(data) ?: error("no queue for $id: $data")
                                        val latch = requests[id] ?: error("no request found for $id: $data")
                                        if (message.isError()) requests.remove(id)
                                        latch.countDown()
                                    }
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
            .flatMap { reqId ->
                val result =
                    findQueue(reqId).poll().asOrFailure<McpInitialize.Response>()
                        .flatMap { input ->
                            notify(McpInitialize.Initialized, McpInitialize.Initialized.Notification)
                                .map { input }
                                .also { tidyUp(reqId) }
                        }
                if (result is Failure<*>) close()
                result
            }
            .map { it.capabilities }
    }

    override fun tools(): McpClient.Tools = ClientTools(::findQueue, ::tidyUp, ::performRequest) { rpc, callback ->
        notificationCallbacks.getOrPut(rpc.Method) { mutableListOf() }.add(callback)
    }

    override fun prompts(): McpClient.Prompts =
        ClientPrompts(::findQueue, ::tidyUp, ::performRequest) { rpc, callback ->
            notificationCallbacks.getOrPut(rpc.Method) { mutableListOf() }.add(callback)
        }

    override fun sampling(): McpClient.Sampling = ClientSampling(::findQueue, ::tidyUp, ::performRequest)

    override fun resources(): McpClient.Resources =
        ClientResources(::findQueue, ::tidyUp, ::performRequest) { rpc, callback ->
            notificationCallbacks.getOrPut(rpc.Method) { mutableListOf() }.add(callback)
        }

    override fun completions(): McpClient.Completions = ClientCompletions(::findQueue, ::tidyUp, ::performRequest)

    protected abstract fun notify(rpc: McpRpc, mcp: ClientMessage.Notification): McpResult<Unit>

    protected abstract fun performRequest(
        rpc: McpRpc, request: ClientMessage, isComplete: (McpNodeType) -> Boolean = { true }
    ): McpResult<RequestId>

    override fun close() {
        running.set(false)
    }

    protected fun tidyUp(requestId: RequestId) {
        requests.remove(requestId)
        messageQueues.remove(requestId)
    }

    protected abstract fun endpoint(it: Event)
    protected abstract fun received(): Sequence<SseMessage>

    private fun findQueue(id: RequestId) = messageQueues[id] ?: error("no queue for $id")
}
