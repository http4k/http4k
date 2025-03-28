package org.http4k.mcp

import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.lens.McpLensTarget
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.McpMessageId
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.Progress
import org.http4k.mcp.model.ProgressToken
import org.http4k.mcp.protocol.messages.HasMeta.Companion.default
import org.http4k.mcp.protocol.messages.McpSampling
import java.time.Duration

/**
 * A tool handler invokes a tool with an input and returns a response
 */
typealias ToolHandler = (ToolRequest) -> ToolResponse
typealias ToolWithClientHandler = (ToolRequest, Client) -> ToolResponse

data class ToolRequest(
    private val args: Map<String, Any> = emptyMap(),
    val progressToken: ProgressToken? = null,
    val connectRequest: Request? = null
) :
    McpLensTarget,
    Map<String, Any> by args

sealed interface ToolResponse {
    val meta: Meta

    data class Ok(val content: List<Content>, override val meta: Meta = default) : ToolResponse {
        constructor(vararg content: Content, meta: Meta = default) : this(content.toList(), meta)
    }

    data class Error(val error: ErrorMessage, override val meta: Meta = default) : ToolResponse
}


interface Client {
    fun receive(id: McpMessageId, response: McpSampling.Response): CompletionStatus
    fun sample(request: SamplingRequest, fetchNextTimeout: Duration? = null): Sequence<McpResult<SamplingResponse>>
    fun report(req: Progress)

    companion object {
        object NoOp : Client {
            override fun receive(id: McpMessageId, response: McpSampling.Response) = error("NoOp")
            override fun sample(request: SamplingRequest, fetchNextTimeout: Duration?) = error("NoOp")
            override fun report(req: Progress) = error("NoOp")
        }
    }
}
