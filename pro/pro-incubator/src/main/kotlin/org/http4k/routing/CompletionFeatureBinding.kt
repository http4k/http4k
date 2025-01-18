package org.http4k.routing

import org.http4k.core.Request
import org.http4k.mcp.CompletionHandler
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.model.CompletionArgument
import org.http4k.mcp.model.Reference
import org.http4k.mcp.protocol.McpCompletion

class CompletionFeatureBinding(val ref: Reference, val handler: CompletionHandler) : FeatureBinding {
    fun complete(ref: Reference, arg: CompletionArgument, connectRequest: Request) =
        handler(CompletionRequest(ref, arg, connectRequest)).let { McpCompletion.Response(it.completion) }
}
