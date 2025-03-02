package org.http4k.mcp.testing.capabilities

import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.map
import org.http4k.mcp.SamplingHandler
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
        with(request) {
            sender(
                McpSampling, McpSampling.Request(
                    messages, maxTokens,
                    systemPrompt, includeContext,
                    temperature, stopSequences,
                    modelPreferences, metadata
                )
            )
        }

        return sequence {
            while (true) {
                val nextEvent = client.nextEvent<McpSampling.Response, SamplingResponse> {
                    SamplingResponse(model, role, content, stopReason)
                }
                yield(nextEvent)
                when {
                    nextEvent is Success -> if (nextEvent.value.stopReason != null) break
                }
            }
        }
    }

    private val onSampling = mutableListOf<SamplingHandler>()

    override fun onSampled(overrideDefaultTimeout: Duration?, fn: SamplingHandler) {
        onSampling.add(fn)
    }

    fun processSamplingRequest() {
        client.nextEvent<McpSampling.Request, SamplingRequest> {
            SamplingRequest(
                messages, maxTokens,
                systemPrompt, includeContext,
                temperature, stopSequences,
                modelPreferences, metadata
            )
        }.map { next ->
            onSampling.forEach { it(next) }
        }
    }
}
