package org.http4k.mcp

import org.http4k.core.Request
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.MaxTokens
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.model.ModelPreferences
import org.http4k.mcp.model.Role
import org.http4k.mcp.model.SamplingIncludeContext
import org.http4k.mcp.model.StopReason
import org.http4k.mcp.model.Temperature

/**
 *  Processes a sampling request from an MCP client
 */
typealias IncomingSamplingHandler = (SamplingRequest) -> Sequence<SamplingResponse>

/**
 * Processes a sampling response from an MCP client
 */
typealias OutgoingSamplingHandler = (SamplingResponse) -> Unit

data class SamplingRequest(
    val messages: List<Message>,
    val maxTokens: MaxTokens,
    val systemPrompt: String? = null,
    val includeContext: SamplingIncludeContext? = null,
    val temperature: Temperature? = null,
    val stopSequences: List<String>? = null,
    val modelPreferences: ModelPreferences? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val connectRequest: Request? = null
)

data class SamplingResponse(
    val model: ModelIdentifier,
    val stopReason: StopReason?,
    val role: Role,
    val content: Content
)
