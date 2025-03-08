package org.http4k.mcp.client.internal

import org.http4k.mcp.SamplingHandler
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.protocol.messages.McpSampling
import org.http4k.mcp.util.McpNodeType
import java.time.Duration
import kotlin.random.Random

internal class ClientSampling(
    private val tidyUp: (RequestId) -> Unit,
    private val defaultTimeout: Duration,
    private val sender: McpRpcSender,
    private val register: (McpRpc, McpCallback<*>) -> Any
) : McpClient.Sampling {

    override fun onSampled(overrideDefaultTimeout: Duration?, fn: SamplingHandler) {
        register(McpSampling, McpCallback(McpSampling.Request::class) { it, requestId ->
            val responses = fn(
                SamplingRequest(
                    it.messages, it.maxTokens, it.systemPrompt, it.includeContext,
                    it.temperature, it.stopSequences, it.modelPreferences, it.metadata
                )
            )
            responses.forEach {
                with(it) {
                    sender(
                        McpSampling, McpSampling.Response(model, stopReason, role, content),
                        overrideDefaultTimeout ?: defaultTimeout,
                        requestId!!
                    )
                    if(stopReason != null) tidyUp(requestId)
                }

            }
        })
    }
}
