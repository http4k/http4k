package org.http4k.mcp.client.internal

import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.messages.McpSampling
import org.http4k.mcp.util.McpNodeType
import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.AtomicReference

internal class ClientSampling(
    private val queueFor: (RequestId) -> BlockingQueue<McpNodeType>,
    private val tidyUp: (RequestId) -> Unit,
    private val sender: McpRpcSender
) : McpClient.Sampling {
    override fun sample(name: ModelIdentifier, request: SamplingRequest): Sequence<Result<SamplingResponse>> {
        fun hasStopReason(message: McpNodeType) = message.asAOrThrow<SamplingResponse>().stopReason != null

        val requestId = AtomicReference<RequestId>(null)
        val messages = sender(
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
        )
            .mapCatching { reqId -> queueFor(reqId).also { requestId.set(reqId) } }
            .getOrThrow()

        return sequence {
            while (true) {
                val message = messages.take()
                yield(
                    runCatching { message.asAOrThrow<McpSampling.Response>() }
                        .map { SamplingResponse(it.model, it.stopReason, it.role, it.content) }
                )

                if (hasStopReason(message)) {
                    tidyUp(requestId.get())
                    break
                }
            }
        }
    }
}
