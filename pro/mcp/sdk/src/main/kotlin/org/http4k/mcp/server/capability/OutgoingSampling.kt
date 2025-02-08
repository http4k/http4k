package org.http4k.mcp.server.capability

import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.CompletionStatus.Finished
import org.http4k.mcp.model.CompletionStatus.InProgress
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.protocol.messages.McpSampling
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles protocol traffic for sampling from the MCP Client.
 */
class OutgoingSampling(private val list: List<OutgoingSamplingCapability>) {

    constructor(vararg list: OutgoingSamplingCapability) : this(list.toList())

    private val subscriptions =
        ConcurrentHashMap<Pair<McpEntity, SessionId>, (McpSampling.Request, RequestId) -> Unit>()

    fun respond(entity: McpEntity, response: McpSampling.Response): CompletionStatus {
        list.find { entity == it.toEntity() }?.process(response)
        return if (response.stopReason != null) Finished else InProgress
    }

    fun sample(entity: McpEntity, request: SamplingRequest, id: RequestId) {
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
                    id
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
