package org.http4k.mcp.capability

import org.http4k.core.Request
import org.http4k.mcp.CompletionHandler
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.model.Reference
import org.http4k.mcp.protocol.McpCompletion

class CompletionBinding(private val ref: Reference, private val handler: CompletionHandler) : CapabilityBinding {
    fun toReference() = ref

    fun complete(mcp: McpCompletion.Request, http: Request) =
        handler(CompletionRequest(mcp.ref, mcp.argument, http))
            .let { McpCompletion.Response(it.completion) }
}
