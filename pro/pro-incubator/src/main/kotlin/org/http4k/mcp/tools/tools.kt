package org.http4k.mcp.tools

import org.http4k.connect.mcp.HasMeta.Companion.default
import org.http4k.connect.mcp.Meta
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.mcp.prompts.Content

typealias ToolHandler<Input> = (ToolRequest<Input>) -> ToolResponse

data class ToolRequest<Input>(val input: Input, val connectRequest: Request)

sealed interface ToolResponse {
    val meta: Meta

    data class Ok(val content: List<Content>, override val meta: Meta = default) : ToolResponse
    data class Error(val error: ErrorMessage, override val meta: Meta = default) : ToolResponse
}
