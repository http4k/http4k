package org.http4k.mcp.protocol

import org.http4k.mcp.model.Completion
import org.http4k.mcp.model.CompletionArgument
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.Reference
import org.http4k.mcp.protocol.HasMeta.Companion.default

object McpCompletion : HasMethod {
    override val Method = McpRpcMethod.of("completion/complete")

    data class Request(
        val ref: Reference,
        val argument: CompletionArgument,
        override val _meta: Meta = default
    ) : ClientMessage.Request, HasMeta

    data class Response(
        val completion: Completion,
        override val _meta: Meta = default
    ) : ServerMessage.Response,
        HasMeta
}
