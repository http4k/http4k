package org.http4k.mcp.features

import org.http4k.core.Request
import org.http4k.mcp.protocol.McpCompletion
import org.http4k.routing.CompletionFeatureBinding

class Completions(private val bindings: List<CompletionFeatureBinding>) : McpFeature {
    fun complete(mcp: McpCompletion.Request, http: Request) =
        bindings.find { it.toReference() == mcp.ref }
            ?.complete(mcp, http)
            ?: error("no completion")
}
