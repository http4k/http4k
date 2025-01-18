package org.http4k.mcp

import org.http4k.core.Request
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.MaxTokens
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.ModelName
import org.http4k.mcp.model.ModelPreferences
import org.http4k.mcp.model.Role
import org.http4k.mcp.model.SamplingIncludeContext
import org.http4k.mcp.model.StopReason
import org.http4k.mcp.model.Temperature

/**
 * A sampling handler is responsible for completing a model request
 */
typealias SamplingHandler = (SampleRequest) -> SampleResponse

data class SampleRequest(
    val messages: List<Message>,
    val maxTokens: MaxTokens,
    val systemPrompt: String? = null,
    val includeContext: SamplingIncludeContext? = null,
    val temperature: Temperature? = null,
    val stopSequences: List<String>? = null,
    val modelPreferences: ModelPreferences? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val connectRequest: Request
)

data class SampleResponse(
    val model: ModelName,
    val stopReason: StopReason,
    val role: Role,
    val content: Content
)
