package org.http4k.ai.mcp.client

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.ai.mcp.McpError
import org.http4k.ai.mcp.McpError.Timeout
import org.http4k.ai.mcp.McpResult
import org.http4k.ai.mcp.client.internal.ClientCompletions
import org.http4k.ai.mcp.client.internal.ClientElicitations
import org.http4k.ai.mcp.client.internal.ClientPrompts
import org.http4k.ai.mcp.client.internal.ClientRequestProgress
import org.http4k.ai.mcp.client.internal.ClientResources
import org.http4k.ai.mcp.client.internal.ClientSampling
import org.http4k.ai.mcp.client.internal.ClientTasks
import org.http4k.ai.mcp.client.internal.ClientTools
import org.http4k.ai.mcp.client.internal.McpCallback
import org.http4k.ai.mcp.client.internal.asOrFailure
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.ServerCapabilities
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.protocol.messages.ClientMessage
import org.http4k.ai.mcp.protocol.messages.McpInitialize
import org.http4k.ai.mcp.protocol.messages.McpRpc
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Event
import java.time.Duration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.random.Random

abstract class AbstractMcpClient(
    private val clientInfo: VersionedMcpEntity,
    private val capabilities: ClientCapabilities,
    protected val protocolVersion: ProtocolVersion = LATEST_VERSION,
    private val defaultTimeout: Duration,
    private val random: Random
) : McpClient {
    private val running = AtomicBoolean(false)
    protected val requests = ConcurrentHashMap<McpMessageId, CountDownLatch>()
    private val callbacks = mutableMapOf<McpRpcMethod, MutableList<McpCallback<*>>>()
    protected val messageQueues = ConcurrentHashMap<McpMessageId, BlockingQueue<McpNodeType>>()

    override fun start(overrideDefaultTimeout: Duration?): McpResult<McpInitialize.Response> {
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
                        else -> with(McpJson) {
                            val data = parse(it.data) as MoshiObject

                            when {
                                data["method"] != null -> {
                                    val message = JsonRpcRequest(this, data.attributes)
                                    val id = message.id?.let { asA<McpMessageId>(compact(it)) }
                                    callbacks[McpRpcMethod.of(message.method)]?.forEach { it(message, id) }
                                }

                                else -> {
                                    val message = JsonRpcResult(this, data.attributes)
                                    val id = asA<McpMessageId>(compact(message.id ?: nullNode()))
                                    messageQueues[id]?.offer(data) ?: error("no queue for $id: $data")
                                    val latch = requests[id] ?: error("no request found for $id: $data")
                                    if (message.isError()) requests.remove(id)
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

        return resultFrom {
            if (!startLatch.await(defaultTimeout.toMillis(), MILLISECONDS)) error("Timeout waiting for endpoint")
        }
            .mapFailure { Timeout }
            .flatMap {
                sendMessage(
                    McpInitialize,
                    McpInitialize.Request(clientInfo, capabilities, protocolVersion),
                    defaultTimeout,
                    McpMessageId.random(random)
                )
                    .flatMap { reqId ->
                        val next = findQueue(reqId)
                            .poll(defaultTimeout.toMillis(), MILLISECONDS)
                            ?.asOrFailure<McpInitialize.Response>()

                        when (next) {
                            null -> Failure(Timeout)
                            else -> next
                                .flatMap { input ->
                                    notify(McpInitialize.Initialized, McpInitialize.Initialized.Notification)
                                        .map { input }
                                        .also { tidyUp(reqId) }
                                }
                        }
                            .mapFailure {
                                close()
                                it
                            }
                    }
            }
    }

    override fun tools(): McpClient.Tools =
        ClientTools(::findQueue, ::tidyUp, ::sendMessage, random, defaultTimeout) { rpc, callback ->
            callbacks.getOrPut(rpc.Method) { mutableListOf() }.add(callback)
        }

    override fun prompts(): McpClient.Prompts =
        ClientPrompts(::findQueue, ::tidyUp, defaultTimeout, ::sendMessage, random) { rpc, callback ->
            callbacks.getOrPut(rpc.Method) { mutableListOf() }.add(callback)
        }

    override fun sampling(): McpClient.Sampling =
        ClientSampling(::tidyUp, defaultTimeout, ::sendMessage) { rpc, callback ->
            callbacks.getOrPut(rpc.Method) { mutableListOf() }.add(callback)
        }

    override fun elicitations(): McpClient.Elicitations =
        ClientElicitations(::tidyUp, defaultTimeout, ::sendMessage) { rpc, callback ->
            callbacks.getOrPut(rpc.Method) { mutableListOf() }.add(callback)
        }

    override fun progress(): McpClient.RequestProgress =
        ClientRequestProgress { rpc, callback ->
            callbacks.getOrPut(rpc.Method) { mutableListOf() }.add(callback)
        }

    override fun resources(): McpClient.Resources =
        ClientResources(::findQueue, ::tidyUp, defaultTimeout, ::sendMessage, random) { rpc, callback ->
            callbacks.getOrPut(rpc.Method) { mutableListOf() }.add(callback)
        }

    override fun completions(): McpClient.Completions =
        ClientCompletions(::findQueue, ::tidyUp, defaultTimeout, ::sendMessage, random)

    override fun tasks(): McpClient.Tasks =
        ClientTasks(::findQueue, ::tidyUp, ::sendMessage, random, defaultTimeout) { rpc, callback ->
            callbacks.getOrPut(rpc.Method) { mutableListOf() }.add(callback)
        }

    protected abstract fun notify(rpc: McpRpc, mcp: ClientMessage.Notification): McpResult<Unit>

    protected abstract fun sendMessage(
        rpc: McpRpc,
        message: ClientMessage,
        timeout: Duration,
        messageId: McpMessageId,
        isComplete: (McpNodeType) -> Boolean = { true }
    ): McpResult<McpMessageId>

    override fun close() {
        running.set(false)
    }

    private fun tidyUp(messageId: McpMessageId) {
        requests.remove(messageId)
        messageQueues.remove(messageId)
    }

    protected fun McpError.failWith(messageId: McpMessageId): Result4k<Nothing, McpError> {
        tidyUp(messageId)
        return Failure(this)
    }

    protected abstract fun endpoint(it: Event)
    protected abstract fun received(): Sequence<SseMessage>

    private fun findQueue(id: McpMessageId) = messageQueues[id] ?: error("no queue for $id")
}
