package org.http4k.mcp

import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Meta
import org.http4k.mcp.protocol.HasMeta.Companion.default

/**
 * A tool handler invokes a tool with an input and returns a response
 */
typealias ToolHandler = (ToolRequest) -> ToolResponse

data class ToolRequest(val args: Map<String, Any>, val connectRequest: Request)

sealed interface ToolResponse {
    val meta: Meta

    data class Ok(val content: List<Content>, override val meta: Meta = default) : ToolResponse
    data class Error(val error: ErrorMessage, override val meta: Meta = default) : ToolResponse
}
