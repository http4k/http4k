//package org.http4k.mcp.testing.capabilities
//
//import dev.forkhandles.result4k.map
//import org.http4k.mcp.SamplingHandler
//import org.http4k.mcp.SamplingRequest
//import org.http4k.mcp.client.McpClient
//import org.http4k.mcp.protocol.messages.McpSampling
//import org.http4k.mcp.testing.nextEvent
//import org.http4k.testing.TestSseClient
//import java.time.Duration
//import java.util.concurrent.atomic.AtomicReference
//import kotlin.concurrent.thread
//
//class TestMcpClientSampling(private val send: TestMcpSender, private val client: AtomicReference<TestSseClient>) :
//    McpClient.Sampling {
//
//    /**
//     * Start the sampling loop
//     */
//    fun start(sleepTime: Duration = Duration.ofMillis(100)) {
//        thread(isDaemon = true) {
//            while (true) {
//                runCatching {
//                    client.nextEvent<McpSampling.Request, SamplingRequest> {
//                        SamplingRequest(
//                            messages, maxTokens,
//                            systemPrompt, includeContext,
//                            temperature, stopSequences,
//                            modelPreferences, metadata
//                        )
//                    }.map { next ->
//                        onSampling.forEach {
//                            it(next.second).forEach {
//                                sender(
//                                    with(it) { McpSampling.Response(model, stopReason, role, content) },
//                                    next.first!!
//                                )
//                            }
//                        }
//                    }
//                }
//                Thread.sleep(sleepTime)
//            }
//        }
//    }
//
//    private val onSampling = mutableListOf<SamplingHandler>()
//
//    override fun onSampled(overrideDefaultTimeout: Duration?, fn: SamplingHandler) {
//        onSampling.add(fn)
//    }
//}
