package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.UserPrompt
import org.http4k.ai.model.Role
import org.http4k.ai.model.StopReason
import org.http4k.ai.model.SystemPrompt
import org.http4k.ai.model.Temperature
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.ModelPreferences
import org.http4k.ai.mcp.model.SamplingIncludeContext
import org.http4k.ai.mcp.protocol.McpRpcMethod
import se.ansman.kotshi.JsonSerializable

object McpSampling : McpRpc {
    override val Method = McpRpcMethod.of("sampling/createMessage")

    @JsonSerializable
    data class Request(
        val messages: List<Message>,
        val maxTokens: MaxTokens,
        val systemPrompt: SystemPrompt? = null,
        val includeContext: SamplingIncludeContext? = null,
        val temperature: Temperature? = null,
        val stopSequences: List<String>? = null,
        val modelPreferences: ModelPreferences? = null,
        val metadata: Map<String, Any> = emptyMap(),
        override val _meta: Meta = Meta.default
    ) : ServerMessage.Request, ClientMessage.Request, HasMeta

    @JsonSerializable
    data class Response(
        val model: ModelName,
        val stopReason: StopReason?,
        val role: Role,
        val content: Content
    ) : ServerMessage.Response, ClientMessage.Response
}

