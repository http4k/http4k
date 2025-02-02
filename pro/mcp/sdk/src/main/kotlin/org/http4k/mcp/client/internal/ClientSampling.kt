package org.http4k.mcp.client.internal

import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.messages.McpSampling
import org.http4k.sse.SseMessage
import java.util.concurrent.BlockingQueue

internal class ClientSampling(
    private val queueFor: (RequestId) -> BlockingQueue<SseMessage.Event>,
    private val sender: McpRpcSender
) : McpClient.Sampling {
    override fun sample(name: ModelIdentifier, request: SamplingRequest): Sequence<Result<SamplingResponse>> {
        fun hasStopReason(message: SseMessage.Event) = message.data.replace(" ", "").contains(""""stopReason":"""")

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
        ).map(queueFor).getOrThrow()

        return sequence {
            while (true) {
                val message = messages.take()
                yield(
                    runCatching { message.asAOrThrow<McpSampling.Response>() }
                        .map { SamplingResponse(it.model, it.stopReason, it.role, it.content) }
                )

                if (hasStopReason(message)) {
                    break
                }
            }
        }
    }
}
