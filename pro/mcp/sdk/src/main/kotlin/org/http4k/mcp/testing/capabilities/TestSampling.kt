package org.http4k.mcp.testing.capabilities

import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.testing.TestMcpSender
import org.http4k.testing.TestSseClient
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

class TestSampling(private val sender: TestMcpSender, private val client: AtomicReference<TestSseClient>) :
    McpClient.Sampling {
    override fun sample(
        name: ModelIdentifier,
        request: SamplingRequest,
        fetchNextTimeout: Duration?
    ): Sequence<McpResult<SamplingResponse>> {
        TODO("Not yet implemented")
    }
}
