package org.http4k.mcp.client.internal

import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.messages.McpSampling
import org.http4k.mcp.util.McpNodeType
import java.util.concurrent.BlockingQueue

internal class ClientSampling(
    private val queueFor: (RequestId) -> BlockingQueue<McpNodeType>,
    private val sender: McpRpcSender
) : McpClient.Sampling {
    override fun sample(name: ModelIdentifier, request: SamplingRequest): Sequence<Result<SamplingResponse>> {
        fun hasStopReason(message: McpNodeType) = runCatching {
            message.asAOrThrow<McpSampling.Response>().stopReason != null
        }.getOrDefault(false)

        return sequence {
            sender(
                McpSampling,
                with(request) {
                    McpSampling.Request(
                        messages, maxTokens, systemPrompt, includeContext,
                        temperature, stopSequences, modelPreferences, metadata
                    )
                },
                ::hasStopReason
            ).onSuccess { reqId ->
                while (true) {
                    val message = queueFor(reqId).take()
                    runCatching { message.asAOrThrow<McpSampling.Response>() }
                        .onSuccess {
                            yield(Result.success(SamplingResponse(it.model, it.stopReason, it.role, it.content)))
                        }
                    if (hasStopReason(message)) return@sequence
                }
            }
        }
    }
}
