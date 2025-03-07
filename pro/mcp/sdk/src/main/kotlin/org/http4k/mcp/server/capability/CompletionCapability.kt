package org.http4k.mcp.server.capability

import org.http4k.core.Request
import org.http4k.mcp.CompletionHandler
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.model.Reference
import org.http4k.mcp.protocol.messages.McpCompletion

interface CompletionCapability : ServerCapability {
    fun toReference(): Reference
    fun complete(mcp: McpCompletion.Request, http: Request): McpCompletion.Response
}

fun CompletionCapability(ref: Reference, handler: CompletionHandler) = object : CompletionCapability {
    override fun toReference() = ref

    override fun complete(mcp: McpCompletion.Request, http: Request) =
        handler(CompletionRequest(mcp.ref, mcp.argument, http))
            .let { McpCompletion.Response(it.completion) }
}
