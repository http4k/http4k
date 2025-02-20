package org.http4k.mcp.client.internal

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.onFailure
import dev.forkhandles.result4k.valueOrNull
import org.http4k.format.MoshiObject
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.messages.McpSampling
import org.http4k.mcp.util.McpJson.asA
import org.http4k.mcp.util.McpJson.compact
import org.http4k.mcp.util.McpJson.nullNode
import org.http4k.mcp.util.McpNodeType
import java.util.concurrent.BlockingQueue

internal class ClientSampling(
    private val queueFor: (RequestId) -> BlockingQueue<McpNodeType>,
    private val tidyUp: (RequestId) -> Unit,
    private val sender: McpRpcSender
) : McpClient.Sampling {
    override fun sample(name: ModelIdentifier, request: SamplingRequest): Sequence<McpResult<SamplingResponse>> {
        fun hasStopReason(message: McpNodeType) =
            message.asOrFailure<SamplingResponse>().valueOrNull()?.stopReason != null

        val queue = sender(
            McpSampling, with(request) {
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
        ).map(queueFor)
            .onFailure { return emptySequence() }

        return sequence {
            while (true) {
                val message = queue.take()
                yield(
                    message.asOrFailure<McpSampling.Response>()
                        .map { SamplingResponse(it.model, it.role, it.content, it.stopReason) }
                )

                if (hasStopReason(message)) {
                    tidyUp(asA<RequestId>(compact((message as MoshiObject)["id"] ?: nullNode())))
                    break
                }
            }
        }
    }
}
