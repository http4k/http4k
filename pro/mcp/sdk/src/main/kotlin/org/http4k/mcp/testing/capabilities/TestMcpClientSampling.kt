package org.http4k.mcp.testing.capabilities

import dev.forkhandles.result4k.valueOrNull
import org.http4k.mcp.SamplingHandler
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.protocol.messages.McpSampling
import org.http4k.mcp.testing.TestMcpSender
import org.http4k.mcp.testing.nextEvent
import org.http4k.testing.TestSseClient
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class TestMcpClientSampling(private val sender: TestMcpSender, private val client: AtomicReference<TestSseClient>) :
    McpClient.Sampling {

    fun start() {
        thread {
            while (true) {
                runCatching {
                    val next = client.nextEvent<McpSampling.Request, SamplingRequest> {
                        SamplingRequest(
                            messages, maxTokens,
                            systemPrompt, includeContext,
                            temperature, stopSequences,
                            modelPreferences, metadata
                        )
                    }.valueOrNull() ?: error("no message received")
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

    private val onSampling = mutableListOf<SamplingHandler>()

    override fun onSampled(overrideDefaultTimeout: Duration?, fn: SamplingHandler) {
        onSampling.add(fn)
    }
}
