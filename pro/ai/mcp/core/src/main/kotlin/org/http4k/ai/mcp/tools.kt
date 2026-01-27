package org.http4k.ai.mcp

import org.http4k.ai.mcp.Client.Companion.NoOp
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Meta.Companion.default
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskMeta
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.lens.McpLensTarget

/**
 * A tool handler invokes a tool with an input and returns a response
 */
typealias ToolHandler = (ToolRequest) -> ToolResponse

fun interface ToolFilter {
    operator fun invoke(handler: ToolHandler): ToolHandler

    companion object
}

val ToolFilter.Companion.NoOp: ToolFilter get() = ToolFilter { it }

fun ToolFilter.then(next: ToolFilter): ToolFilter = ToolFilter { this(next(it)) }

fun ToolFilter.then(next: ToolHandler): ToolHandler = this(next)

data class ToolRequest(
    val args: Map<String, Any> = emptyMap(),
    override val meta: Meta = default,
    override val task: TaskMeta? = null,
    val client: Client = NoOp,
    val connectRequest: Request? = null
) : CapabilityRequest,
    McpLensTarget,
    Map<String, Any> by args

sealed interface ToolResponse {
    val meta: Meta

    data class Ok(
        val content: List<Content>? = null,
        val structuredContent: McpNodeType? = null,
        override val meta: Meta = default
    ) : ToolResponse, McpLensTarget {
        constructor(structuredContent: McpNodeType, meta: Meta = default) : this(listOf(Text(McpJson.asFormatString(structuredContent))), structuredContent, meta)
        constructor(vararg content: Content, meta: Meta = default) : this(content.toList(), null, meta)
        constructor(vararg content: String, meta: Meta = default) : this(content.map(::Text).toList(), null, meta)
    }

    data class Error(val error: ErrorMessage, override val meta: Meta = default) : ToolResponse {
        constructor(code: Int, message: String, meta: Meta = default) : this(ErrorMessage(code, message), meta)
    }

    data class Task(val task: org.http4k.ai.mcp.model.Task, override val meta: Meta = default) : ToolResponse
}
