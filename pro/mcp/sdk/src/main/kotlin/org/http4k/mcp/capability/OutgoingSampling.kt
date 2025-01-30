package org.http4k.mcp.capability

import org.http4k.mcp.SampleRequest
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.Done
import org.http4k.mcp.protocol.Done.YES
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.protocol.messages.McpSampling
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles protocol traffic for sampling from the MCP Client.
 */
class OutgoingSampling(private val list: List<OutgoingSamplingCapability>) {

    private val subscriptions =
        ConcurrentHashMap<Pair<McpEntity, SessionId>, (McpSampling.Request, RequestId) -> Unit>()

    fun respond(entity: McpEntity, response: McpSampling.Response): Done {
        list.find { entity == it.toEntity() }?.process(response)
        return if (response.stopReason != null) YES else Done.NO
    }

    fun sample(entity: McpEntity, request: SampleRequest) {
        with(request) {
            subscriptions[subscriptions.keys.filter { it.first == entity }.random()]
                ?.invoke(
                    McpSampling.Request(
                        messages,
                        maxTokens,
                        systemPrompt,
                        includeContext,
                        temperature,
                        stopSequences,
                        modelPreferences,
                        metadata
                    ),
                    requestId,
                )
        }
    }

    fun onRequest(sessionId: SessionId, entity: McpEntity, fn: (McpSampling.Request, RequestId) -> Unit) {
        subscriptions[entity to sessionId] = fn
    }

    fun remove(sessionId: SessionId, entity: McpEntity) {
        subscriptions.remove(entity to sessionId)
    }
}
