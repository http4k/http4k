package org.http4k.ai.mcp.server.capability

import org.http4k.core.Request
import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.CompletionHandler
import org.http4k.ai.mcp.CompletionRequest
import org.http4k.ai.mcp.model.Completion
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.protocol.messages.McpCompletion

interface CompletionCapability : ServerCapability, CompletionHandler {
    fun toReference(): Reference
    fun complete(mcp: McpCompletion.Request, client: Client, http: Request): McpCompletion.Response
}

fun CompletionCapability(ref: Reference, handler: CompletionHandler) = object : CompletionCapability {
    override fun toReference() = ref

    override fun complete(mcp: McpCompletion.Request, client: Client, http: Request) =
        handler(CompletionRequest(mcp.argument, mcp.context, mcp._meta, client, http))
            .let { McpCompletion.Response(Completion(it.values, it.total, it.hasMore)) }

    override fun invoke(p1: CompletionRequest) = handler(p1)
}
