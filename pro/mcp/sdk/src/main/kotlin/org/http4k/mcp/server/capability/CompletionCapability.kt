package org.http4k.mcp.server.capability

import org.http4k.core.Request
import org.http4k.mcp.CompletionHandler
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.CompletionWithClientHandler
import org.http4k.mcp.model.Completion
import org.http4k.mcp.model.Reference
import org.http4k.mcp.protocol.messages.McpCompletion
import org.http4k.mcp.server.protocol.Client
import org.http4k.mcp.server.protocol.Client.Companion.NoOp

interface CompletionCapability : ServerCapability, CompletionWithClientHandler, CompletionHandler {
    fun toReference(): Reference
    fun complete(mcp: McpCompletion.Request, client: Client, http: Request): McpCompletion.Response
}

fun CompletionCapability(ref: Reference, handler: CompletionHandler) =
    CompletionCapability(ref) { request, _ -> handler(request) }

fun CompletionCapability(ref: Reference, handler: CompletionWithClientHandler) = object : CompletionCapability {
    override fun toReference() = ref

    override fun complete(mcp: McpCompletion.Request, client: Client, http: Request) =
        handler(CompletionRequest(mcp.ref, mcp.argument, http), client)
            .let { McpCompletion.Response(Completion(it.values, it.total, it.hasMore)) }

    override fun invoke(p1: CompletionRequest) = handler(p1, NoOp)
    override fun invoke(p1: CompletionRequest, client: Client) = handler(p1, client)
}
