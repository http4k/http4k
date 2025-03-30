package org.http4k.mcp.testing.capabilities

import dev.forkhandles.result4k.map
import org.http4k.mcp.SamplingHandler
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.protocol.messages.McpSampling
import org.http4k.mcp.testing.TestMcpSender
import org.http4k.mcp.testing.nextEvent
import java.time.Duration
import kotlin.concurrent.thread

class TestingSampling(sender: TestMcpSender) : McpClient.Sampling {

    private val onSampling = mutableListOf<SamplingHandler>()

    override fun onSampled(overrideDefaultTimeout: Duration?, fn: SamplingHandler) {
        onSampling.add(fn)
    }

    init {
        thread(isDaemon = true) {
            while (true) {
                runCatching {
                    sender.stream().nextEvent<McpSampling.Request, SamplingRequest> {
                        SamplingRequest(
                            messages, maxTokens,
                            systemPrompt, includeContext,
                            temperature, stopSequences,
                            modelPreferences, metadata
                        )
                    }.map { next ->
                        onSampling.forEach {
                            it(next.second).forEach {
                                sender(
                                    with(it) { McpSampling.Response(model, stopReason, role, content) },
                                    next.first!!
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
