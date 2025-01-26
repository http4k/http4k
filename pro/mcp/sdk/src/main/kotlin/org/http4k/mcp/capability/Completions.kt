package org.http4k.mcp.capability

import org.http4k.core.Request
import org.http4k.mcp.protocol.McpCompletion

class Completions(private val bindings: List<CompletionBinding>) {
    fun complete(mcp: McpCompletion.Request, http: Request) =
        bindings.find { it.toReference() == mcp.ref }
            ?.complete(mcp, http)
            ?: error("no completion")
}
