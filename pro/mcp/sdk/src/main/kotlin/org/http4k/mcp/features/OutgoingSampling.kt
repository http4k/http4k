package org.http4k.mcp.features

import org.http4k.mcp.SampleRequest
import org.http4k.mcp.protocol.McpEntity
import org.http4k.mcp.protocol.McpSampling
import org.http4k.mcp.protocol.SessionId
import org.http4k.routing.OutgoingSamplingFeatureBinding
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles protocol traffic for sampling from the MCP Client.
 */
class OutgoingSampling(private val list: List<OutgoingSamplingFeatureBinding>) : McpFeature {

    private val subscriptions = ConcurrentHashMap<Pair<McpEntity, SessionId>, (McpSampling.Request) -> Unit>()

    fun respond(entity: McpEntity, response: McpSampling.Response) {
        list.find { entity == it.toEntity() }?.process(response)
    }

    fun sample(entity: McpEntity, request: SampleRequest) {
        subscriptions[subscriptions.keys.filter { it.first == entity }.random()]
            ?.invoke(
                with(request) {
                    McpSampling.Request(
                        messages,
                        maxTokens,
                        systemPrompt,
                        includeContext,
                        temperature,
                        stopSequences,
                        modelPreferences,
                        metadata
                    )
                }
            )
    }

    fun onRequest(sessionId: SessionId, entity: McpEntity, fn: (McpSampling.Request) -> Unit) {
        subscriptions[entity to sessionId] = fn
    }

    fun remove(sessionId: SessionId, entity: McpEntity) {
        subscriptions.remove(entity to sessionId)
    }
}
