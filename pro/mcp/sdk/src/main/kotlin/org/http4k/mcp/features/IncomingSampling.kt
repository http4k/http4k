package org.http4k.mcp.features

import org.http4k.core.Request
import org.http4k.mcp.protocol.McpSampling
import org.http4k.routing.IncomingSamplingFeatureBinding

/**
 * Handles protocol traffic for sampling. Selects the best model to serve a request.
 */
class IncomingSampling(private val list: List<IncomingSamplingFeatureBinding>) : McpFeature {

    fun sample(mcp: McpSampling.Request, http: Request) =
        mcp.selectModel()?.sample(mcp, http) ?: error("No model to serve request")

    private fun McpSampling.Request.selectModel() = when {
        modelPreferences == null -> list.firstOrNull()
        else -> list.maxByOrNull { it.toModelSelector().score(modelPreferences) }
    }
}
