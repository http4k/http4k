package org.http4k.mcp.testing.capabilities

import dev.forkhandles.result4k.valueOrNull
import org.http4k.mcp.SamplingHandler
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.protocol.messages.McpSampling
import org.http4k.mcp.testing.TestMcpSender
import org.http4k.mcp.testing.nextEvent
import java.time.Duration

class TestingSampling(sender: TestMcpSender) : McpClient.Sampling {

    private val onSampling = mutableListOf<SamplingHandler>()

    override fun onSampled(overrideDefaultTimeout: Duration?, fn: SamplingHandler) {
        onSampling.add(fn)
    }

    init {
        sender.on(McpSampling) { event ->
            val (id, req) =
                event.nextEvent<SamplingRequest, McpSampling.Request> {
                    SamplingRequest(
                        messages, maxTokens,
                        systemPrompt, includeContext,
                        temperature, stopSequences,
                        modelPreferences, metadata,
                        _meta.progress,
                        )
                }.valueOrNull()!!
            onSampling.forEach { handler ->
                handler(req).forEach { response ->
                    sender(with(response) { McpSampling.Response(model, stopReason, role, content) }, id!!)
                }
            }
        }
    }
}
