package org.http4k.routing

import org.http4k.core.Request
import org.http4k.mcp.IncomingSamplingHandler
import org.http4k.mcp.SampleRequest
import org.http4k.mcp.model.ModelSelector
import org.http4k.mcp.protocol.McpSampling

class IncomingSamplingFeatureBinding(private val modelSelector: ModelSelector, private val handler: IncomingSamplingHandler) :
    FeatureBinding {

    fun toModelSelector() = modelSelector

    fun sample(req: McpSampling.Request, connectRequest: Request) =
        handler(
            with(req) {
                SampleRequest(
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
        ).let { McpSampling.Response(it.model, it.stopReason, it.role, it.content) }
}
