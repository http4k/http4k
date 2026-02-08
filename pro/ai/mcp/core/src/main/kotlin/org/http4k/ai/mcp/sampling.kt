package org.http4k.ai.mcp

import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.Role
import org.http4k.ai.model.StopReason
import org.http4k.ai.model.SystemPrompt
import org.http4k.ai.model.Temperature
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.ModelPreferences
import org.http4k.ai.mcp.model.ProgressToken
import org.http4k.ai.mcp.model.SamplingIncludeContext
import org.http4k.ai.mcp.model.ToolChoice
import org.http4k.ai.mcp.model.ToolChoiceMode
import org.http4k.ai.mcp.protocol.messages.McpTool

/**
 *  Processes a sampling request from an MCP server to a client
 */
typealias SamplingHandler = (SamplingRequest) -> Sequence<SamplingResponse>

fun interface SamplingFilter {
    operator fun invoke(handler: SamplingHandler): SamplingHandler
    companion object
}

val SamplingFilter.Companion.NoOp: SamplingFilter get() = SamplingFilter { it }

fun SamplingFilter.then(next: SamplingFilter): SamplingFilter = SamplingFilter { this(next(it)) }

fun SamplingFilter.then(next: SamplingHandler): SamplingHandler = this(next)

data class SamplingRequest(
    val messages: List<Message>,
    val maxTokens: MaxTokens,
    val systemPrompt: SystemPrompt? = null,
    val includeContext: SamplingIncludeContext? = null,
    val temperature: Temperature? = null,
    val stopSequences: List<String>? = null,
    val modelPreferences: ModelPreferences? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val tools: List<McpTool> = emptyList(),
    val toolChoice: ToolChoice = ToolChoice(ToolChoiceMode.auto),
    val progressToken: ProgressToken? = null
)

sealed interface SamplingResponse {
    data class Ok(
        val model: ModelName,
        val role: Role,
        val content: List<Content>,
        val stopReason: StopReason? = null
    ) : SamplingResponse

    data class Task(val task: org.http4k.ai.mcp.model.Task) : SamplingResponse
}
