package org.http4k.mcp.testing.capabilities

import dev.forkhandles.result4k.Success
import org.http4k.mcp.IncomingSamplingHandler
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.protocol.messages.McpSampling
import org.http4k.mcp.testing.TestMcpSender
import org.http4k.mcp.testing.nextEvent
import org.http4k.testing.TestSseClient
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

class TestSampling(private val sender: TestMcpSender, private val client: AtomicReference<TestSseClient>) :
    McpClient.Sampling {

    override fun sample(
        name: ModelIdentifier, request: SamplingRequest, fetchNextTimeout: Duration?
    ): Sequence<McpResult<SamplingResponse>> {
        sender(
            McpSampling, McpSampling.Request(
                request.messages, request.maxTokens,
                request.systemPrompt, request.includeContext,
                request.temperature, request.stopSequences, request.modelPreferences, request.metadata
            )
        )

        return sequence {
            while (true) {
                val nextEvent = client.nextEvent<McpSampling.Response, SamplingResponse> {
                    SamplingResponse(this.model, this.role, this.content, this.stopReason)
                }
                yield(nextEvent)
                when {
                    nextEvent is Success -> if (nextEvent.value.stopReason != null) break
                }
            }
        }
    }

    override fun onSampled(overrideDefaultTimeout: Duration?, fn: IncomingSamplingHandler) {
        TODO("Not yet implemented")
    }

}
