package org.http4k.mcp.server.capability

import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound
import org.http4k.mcp.protocol.McpException
import org.http4k.mcp.protocol.messages.McpSampling

/**
 * Handles protocol traffic for sampling. Selects the best model to serve a request.
 */
class IncomingSampling(private val list: List<IncomingSamplingCapability>) {

    constructor(vararg list: IncomingSamplingCapability) : this(list.toList())

    fun sample(mcp: McpSampling.Request, http: Request) =
        mcp.selectModel()?.sample(mcp, http) ?: throw McpException(MethodNotFound)

    private fun McpSampling.Request.selectModel() = when {
        modelPreferences == null -> list.firstOrNull()
        else -> list.maxByOrNull { it.toModelSelector().score(modelPreferences) }
    }
}
