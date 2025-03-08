package org.http4k.mcp.server.protocol

import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.protocol.messages.McpSampling
import java.time.Duration

/**
 * Handles protocol traffic for sampling. Selects the best model to serve a request.
 */
interface Sampling {
    fun receive(id: RequestId, response: McpSampling.Response): CompletionStatus
    fun sampleClient(entity: McpEntity, request: SamplingRequest, fetchNextTimeout: Duration?): Sequence<McpResult<SamplingResponse>>

    fun onSampleClient(sessionId: SessionId, entity: McpEntity, fn: (McpSampling.Request, RequestId) -> Unit)
    fun remove(sessionId: SessionId)
}
