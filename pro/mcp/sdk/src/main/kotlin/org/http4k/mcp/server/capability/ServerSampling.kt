package org.http4k.mcp.server.capability

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.client.McpError.Timeout
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.CompletionStatus.Finished
import org.http4k.mcp.model.CompletionStatus.InProgress
import org.http4k.mcp.model.McpMessageId
import org.http4k.mcp.model.Meta
import org.http4k.mcp.protocol.messages.McpSampling
import org.http4k.mcp.server.protocol.ClientRequestTarget
import org.http4k.mcp.server.protocol.Sampling
import java.time.Duration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Long.Companion.MAX_VALUE
import kotlin.random.Random

class ServerSampling(private val random: Random = Random) : Sampling {
    private val subscriptions =
        ConcurrentHashMap<ClientRequestTarget, (McpSampling.Request, McpMessageId) -> Unit>()
    private val counts = ConcurrentHashMap<ClientRequestTarget, AtomicInteger>()

    private val responseQueues = ConcurrentHashMap<McpMessageId, BlockingQueue<SamplingResponse>>()

    override fun receive(id: McpMessageId, response: McpSampling.Response): CompletionStatus {
        responseQueues[id]?.put(SamplingResponse(response.model, response.role, response.content, response.stopReason))

        return when {
            response.stopReason == null -> InProgress
            else -> {
                responseQueues.remove(id)
                Finished
            }
        }
    }

    override fun onSampleClient(target: ClientRequestTarget, fn: (McpSampling.Request, McpMessageId) -> Unit) {
        subscriptions[target] = fn
        counts.getOrPut(target) { AtomicInteger() }.incrementAndGet()
    }

    override fun sampleClient(
        target: ClientRequestTarget,
        request: SamplingRequest,
        fetchNextTimeout: Duration?
    ): Sequence<McpResult<SamplingResponse>> {
        val queue = LinkedBlockingDeque<SamplingResponse>()
        val id = McpMessageId.random(random)
        responseQueues[id] = queue
        with(request) {
            subscriptions[target]?.invoke(
                McpSampling.Request(
                    messages,
                    maxTokens,
                    systemPrompt,
                    includeContext,
                    temperature,
                    stopSequences,
                    modelPreferences,
                    metadata,
                    _meta = Meta(progressToken)
                ),
                id
            )
        }
        return sequence {
            while (true) {
                when (val nextMessage = queue.poll(fetchNextTimeout?.toMillis() ?: MAX_VALUE, MILLISECONDS)) {
                    null -> {
                        yield(Failure(Timeout))
                        break
                    }

                    else -> {
                        yield(Success(nextMessage))

                        if (nextMessage.stopReason != null) {
                            responseQueues.remove(id)
                            break
                        }
                    }
                }
            }
        }
    }

    override fun remove(target: ClientRequestTarget) {
        if (counts.getOrPut(target) { AtomicInteger(0) }.decrementAndGet() <- 0) {
            subscriptions.remove(target)
            counts.remove(target)
        }
    }
}
