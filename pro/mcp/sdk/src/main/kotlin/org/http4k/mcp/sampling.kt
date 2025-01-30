package org.http4k.mcp

import org.http4k.core.Request
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.MaxTokens
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.model.ModelPreferences
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.model.Role
import org.http4k.mcp.model.SamplingIncludeContext
import org.http4k.mcp.model.StopReason
import org.http4k.mcp.model.Temperature

/**
 *  Processes a sampling request from an MCP client
 */
typealias IncomingSamplingHandler = (SampleRequest) -> Sequence<SampleResponse>

/**
 * Processes a sampling response from an MCP client
 */
typealias OutgoingSamplingHandler = (SampleResponse) -> Unit

data class SampleRequest(
    val messages: List<Message>,
    val maxTokens: MaxTokens,
    val requestId: RequestId,
    val systemPrompt: String? = null,
    val includeContext: SamplingIncludeContext? = null,
    val temperature: Temperature? = null,
    val stopSequences: List<String>? = null,
    val modelPreferences: ModelPreferences? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val connectRequest: Request
)

data class SampleResponse(
    val model: ModelIdentifier,
    val stopReason: StopReason?,
    val role: Role,
    val content: Content
)
