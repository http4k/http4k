package org.http4k.mcp.features

import org.http4k.core.Request
import org.http4k.mcp.model.ModelPreferences
import org.http4k.mcp.protocol.McpSampling
import org.http4k.routing.SamplingFeatureBinding

/**
 * Handles protocol traffic for sampling. Selects the best model to serve a request.
 */
class Sampling(private val list: List<SamplingFeatureBinding>) : McpFeature {

    fun sample(mcp: McpSampling.Request, http: Request) =
        mcp.selectModel()?.sample(mcp, http) ?: error("No model to serve request")

    private fun McpSampling.Request.selectModel() =
        samplingFeatureBinding(modelPreferences)

    private fun samplingFeatureBinding(modelPreferences: ModelPreferences?) = when {
        modelPreferences == null -> list.firstOrNull()
        else -> list.maxByOrNull { it.toModelSelector().score(modelPreferences) }
    }
}
