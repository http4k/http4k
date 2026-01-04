package org.http4k.ai.mcp.client.internal

import org.http4k.ai.mcp.SamplingHandler
import org.http4k.ai.mcp.SamplingRequest
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.messages.McpRpc
import org.http4k.ai.mcp.protocol.messages.McpSampling
import java.time.Duration

internal class ClientSampling(
    private val tidyUp: (McpMessageId) -> Unit,
    private val defaultTimeout: Duration,
    private val sender: McpRpcSender,
    private val register: (McpRpc, McpCallback<*>) -> Any
) : McpClient.Sampling {

    override fun onSampled(overrideDefaultTimeout: Duration?, fn: SamplingHandler) {
        register(McpSampling,
            McpCallback(McpSampling.Request::class) { request, requestId ->
                if (requestId == null) return@McpCallback

                val responses = fn(
                    SamplingRequest(
                        request.messages,
                        request.maxTokens,
                        request.systemPrompt,
                        request.includeContext,
                        request.temperature,
                        request.stopSequences,
                        request.modelPreferences,
                        request.metadata,
                        request.tools ?: emptyList(),
                        request.toolChoice
                    )
                )

                val timeout = overrideDefaultTimeout ?: defaultTimeout

                responses.forEach {
                    sender(
                        McpSampling,
                        McpSampling.Response(it.model, it.stopReason, it.role, it.content),
                        timeout,
                        requestId
                    )

                    if (it.stopReason != null) tidyUp(requestId)
                }
            })
    }
}
