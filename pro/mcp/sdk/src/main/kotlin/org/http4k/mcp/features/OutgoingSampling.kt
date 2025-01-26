package org.http4k.mcp.features

import org.http4k.mcp.SampleRequest
import org.http4k.mcp.protocol.McpSampling
import org.http4k.mcp.protocol.SessionId
import org.http4k.routing.OutgoingSamplingFeatureBinding
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles protocol traffic for sampling from the MCP Client.
 */
class OutgoingSampling(private val list: List<OutgoingSamplingFeatureBinding>) : McpFeature {

    private val subscriptions = ConcurrentHashMap<SessionId, (McpSampling.Request) -> Unit>()

    fun respond(response: McpSampling.Response) {
        list.find { response.model == it.toModel() }?.process(response)
    }

    fun sample(sId: SessionId, request: SampleRequest) {
        subscriptions[sId]?.invoke(
            McpSampling.Request(
                request.messages,
                request.maxTokens,
                request.systemPrompt,
                request.includeContext,
                request.temperature,
                request.stopSequences,
                request.modelPreferences,
                request.metadata
            )
        )
    }

    fun onRequest(sId: SessionId, fn: (McpSampling.Request) -> Unit) {
        subscriptions[sId] = fn
    }

    fun remove(sId: SessionId) {
        subscriptions -= sId
    }
}
