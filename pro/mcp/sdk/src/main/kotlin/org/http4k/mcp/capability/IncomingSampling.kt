package org.http4k.mcp.capability

import org.http4k.core.Request
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.McpSampling

/**
 * Handles protocol traffic for sampling. Selects the best model to serve a request.
 */
class IncomingSampling(private val list: List<IncomingSamplingBinding>) {

    fun sample(mcp: McpSampling.Request, requestId: RequestId, http: Request) =
        mcp.selectModel()?.sample(mcp, requestId, http) ?: error("No model to serve request")

    private fun McpSampling.Request.selectModel() = when {
        modelPreferences == null -> list.firstOrNull()
        else -> list.maxByOrNull { it.toModelSelector().score(modelPreferences) }
    }
}
