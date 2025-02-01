package org.http4k.mcp.capability

import org.http4k.core.Request
import org.http4k.mcp.IncomingSamplingHandler
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.model.ModelSelector
import org.http4k.mcp.protocol.messages.McpSampling

class IncomingSamplingCapability(
    private val modelSelector: ModelSelector,
    private val handler: IncomingSamplingHandler
) : ServerCapability {

    fun toModelSelector() = modelSelector

    fun sample(req: McpSampling.Request, connectRequest: Request) =
        handler(
            with(req) {
                SamplingRequest(
                    messages,
                    maxTokens,
                    systemPrompt,
                    includeContext,
                    temperature,
                    stopSequences,
                    modelPreferences,
                    metadata,
                    connectRequest
                )
            }
        ).map { McpSampling.Response(it.model, it.stopReason, it.role, it.content) }
}
