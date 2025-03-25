package org.http4k.mcp.server.protocol

import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.McpMessageId
import org.http4k.mcp.protocol.messages.McpSampling
import java.time.Duration

/**
 * Handles protocol traffic for sampling. Selects the best model to serve a request.
 */
interface Sampling {
    fun receive(id: McpMessageId, response: McpSampling.Response): CompletionStatus
    fun sampleClient(
        target: ClientRequestTarget,
        request: SamplingRequest,
        fetchNextTimeout: Duration?
    ): Sequence<McpResult<SamplingResponse>>

    fun onSampleClient(target: ClientRequestTarget, fn: (McpSampling.Request, McpMessageId) -> Unit)

    fun remove(target: ClientRequestTarget)
}
