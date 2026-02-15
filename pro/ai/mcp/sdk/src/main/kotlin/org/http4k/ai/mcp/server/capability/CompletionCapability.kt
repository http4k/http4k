package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.CompletionFilter
import org.http4k.ai.mcp.CompletionHandler
import org.http4k.ai.mcp.CompletionRequest
import org.http4k.ai.mcp.model.Completion
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.then
import org.http4k.core.Request

class CompletionCapability(
    internal val ref: Reference,
    internal val handler: CompletionHandler
) : ServerCapability, CompletionHandler {
    fun toReference() = ref

    fun complete(mcp: McpCompletion.Request, client: Client, http: Request) =
        handler(CompletionRequest(mcp.argument, mcp.context, mcp._meta, client, http))
            .let { McpCompletion.Response(Completion(it.values, it.total, it.hasMore)) }

    override fun invoke(p1: CompletionRequest) = handler(p1)
}

fun CompletionFilter.then(capability: CompletionCapability) = CompletionCapability(capability.ref, then(capability))
