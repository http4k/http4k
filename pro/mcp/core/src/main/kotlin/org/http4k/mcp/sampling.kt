package org.http4k.mcp

import org.http4k.connect.model.MaxTokens
import org.http4k.connect.model.ModelName
import org.http4k.connect.model.Role
import org.http4k.connect.model.StopReason
import org.http4k.connect.model.SystemMessage
import org.http4k.connect.model.Temperature
import org.http4k.core.Request
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.ModelPreferences
import org.http4k.mcp.model.ProgressToken
import org.http4k.mcp.model.SamplingIncludeContext

/**
 *  Processes a sampling request from an MCP server to a client
 */
typealias SamplingHandler = (SamplingRequest) -> Sequence<SamplingResponse>

fun interface SamplingFilter {
    operator fun invoke(request: SamplingHandler): SamplingHandler
    companion object
}

val SamplingFilter.Companion.NoOp: SamplingFilter get() = SamplingFilter { it }

fun SamplingFilter.then(next: SamplingFilter): SamplingFilter = SamplingFilter { this(next(it)) }

fun SamplingFilter.then(next: SamplingHandler): SamplingHandler = this(next)

data class SamplingRequest(
    val messages: List<Message>,
    val maxTokens: MaxTokens,
    val systemPrompt: SystemMessage? = null,
    val includeContext: SamplingIncludeContext? = null,
    val temperature: Temperature? = null,
    val stopSequences: List<String>? = null,
    val modelPreferences: ModelPreferences? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val progressToken: ProgressToken? = null
)

data class SamplingResponse(
    val model: ModelName,
    val role: Role,
    val content: Content,
    val stopReason: StopReason? = null
)
