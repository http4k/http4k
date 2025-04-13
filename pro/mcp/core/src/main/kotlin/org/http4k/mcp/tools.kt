package org.http4k.mcp

import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.lens.McpLensTarget
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Content.Text
import org.http4k.mcp.model.Meta
import org.http4k.mcp.Client.Companion.NoOp

/**
 * A tool handler invokes a tool with an input and returns a response
 */
typealias ToolHandler = (ToolRequest) -> ToolResponse

fun interface ToolFilter {
    operator fun invoke(request: ToolHandler): ToolHandler

    companion object
}

val ToolFilter.Companion.NoOp: ToolFilter get() = ToolFilter { it }

fun ToolFilter.then(next: ToolFilter): ToolFilter = ToolFilter { this(next(it)) }

fun ToolFilter.then(next: ToolHandler): ToolHandler = this(next)


data class ToolRequest(
    val args: Map<String, Any> = emptyMap(),
    val meta: Meta = Meta.default,
    val client: Client = NoOp,
    val connectRequest: Request? = null
) :
    McpLensTarget,
    Map<String, Any> by args

sealed interface ToolResponse {
    val meta: Meta

    data class Ok(val content: List<Content>, override val meta: Meta = Meta.default) : ToolResponse {
        constructor(vararg content: Content, meta: Meta = Meta.default) : this(content.toList(), meta)
        constructor(vararg content: String, meta: Meta = Meta.default) : this(content.map(::Text).toList(), meta)
    }

    data class Error(val error: ErrorMessage, override val meta: Meta = Meta.default) : ToolResponse {
        constructor(code: Int, message: String, meta: Meta = Meta.default) : this(ErrorMessage(code, message), meta)
    }
}
