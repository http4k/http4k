package org.http4k.mcp

import org.http4k.connect.mcp.protocol.HasMeta.Companion.default
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Meta

typealias ToolHandler<Input> = (ToolRequest<Input>) -> ToolResponse

data class ToolRequest<Input>(val input: Input, val connectRequest: Request)

sealed interface ToolResponse {
    val meta: Meta

    data class Ok(val content: List<Content>, override val meta: Meta = default) : ToolResponse
    data class Error(val error: ErrorMessage, override val meta: Meta = default) : ToolResponse
}
