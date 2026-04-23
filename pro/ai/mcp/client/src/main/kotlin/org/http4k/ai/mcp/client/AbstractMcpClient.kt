/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
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
import org.http4k.ai.mcp.client.internal.McpCallbackRegistry
import org.http4k.ai.mcp.client.internal.asOrFailure
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.protocol.messages.McpInitialize
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcMessage
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.format.MoshiObject
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Event
import java.time.Duration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

abstract class AbstractMcpClient(
    private val clientInfo: VersionedMcpEntity,
    private val capabilities: ClientCapabilities,
    protected val protocolVersion: ProtocolVersion = LATEST_VERSION,
    private val defaultTimeout: Duration,
) : McpClient {
    private val running = AtomicBoolean(false)
    protected val requests = ConcurrentHashMap<McpMessageId, CountDownLatch>()
    private val registry = McpCallbackRegistry()
    protected val messageQueues = ConcurrentHashMap<McpMessageId, BlockingQueue<McpNodeType>>()

    protected val id = AtomicLong(0)

    override fun start(overrideDefaultTimeout: Duration?): McpResult<McpInitialize.Response.Result> {
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
                                data["method"] != null -> registry.dispatch(asA<McpJsonRpcRequest>(it.data))

                                else -> {
                                    val id = data["id"]?.let { asA<McpMessageId>(compact(it)) }
                                        ?: error("no id in result: $data")
                                    messageQueues[id]?.offer(data) ?: error("no queue for $id: $data")
                                    val latch = requests[id] ?: error("no request found for $id: $data")
                                    if (data["error"] != null) requests.remove(id)
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
                val messageId = McpMessageId.of(id.incrementAndGet())
                sendMessage(
                    McpInitialize.Request(McpInitialize.Request.Params(clientInfo, capabilities, protocolVersion), messageId),
                    defaultTimeout,
                    messageId,
                )
                    .flatMap { reqId ->
                        val next = findQueue(reqId)
                            .poll(defaultTimeout.toMillis(), MILLISECONDS)
                            ?.asOrFailure<McpInitialize.Response.Result>()

                        when (next) {
                            null -> Failure(Timeout)
                            else -> next
                                .flatMap { input ->
                                    notify(McpInitialize.Initialized.Notification(McpInitialize.Initialized.Notification.Params()))
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
        ClientTools(::findQueue, ::tidyUp, ::sendMessage, { McpMessageId.of(id.incrementAndGet())}, defaultTimeout, registry)

    override fun prompts(): McpClient.Prompts =
        ClientPrompts(::findQueue, ::tidyUp, defaultTimeout, ::sendMessage, { McpMessageId.of(id.incrementAndGet())}, registry)

    override fun sampling(): McpClient.Sampling =
        ClientSampling(::tidyUp, defaultTimeout, ::sendMessage, registry)

    override fun elicitations(): McpClient.Elicitations =
        ClientElicitations(::tidyUp, defaultTimeout, ::sendMessage, registry)

    override fun progress(): McpClient.RequestProgress =
        ClientRequestProgress(registry)

    override fun resources(): McpClient.Resources =
        ClientResources(::findQueue, ::tidyUp, defaultTimeout, ::sendMessage, { McpMessageId.of(id.incrementAndGet())}, registry)

    override fun completions(): McpClient.Completions =
        ClientCompletions(::findQueue, ::tidyUp, defaultTimeout, ::sendMessage, { McpMessageId.of(id.incrementAndGet())})

    override fun tasks(): McpClient.Tasks =
        ClientTasks(::findQueue, ::tidyUp, ::sendMessage, { McpMessageId.of(id.incrementAndGet())}, defaultTimeout, registry)

    protected abstract fun notify(message: McpJsonRpcMessage): McpResult<Unit>

    protected abstract fun sendMessage(
        message: McpJsonRpcMessage,
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
