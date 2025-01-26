package org.http4k.routing

import org.http4k.mcp.OutgoingSamplingHandler
import org.http4k.mcp.SampleResponse
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.protocol.McpSampling

class OutgoingSamplingFeatureBinding(
    private val model: ModelIdentifier, private val handler: OutgoingSamplingHandler
) : FeatureBinding {

    fun toModel() = model

    fun process(response: McpSampling.Response) = with(response) {
        handler(SampleResponse(model, stopReason, role, content))
    }
}
