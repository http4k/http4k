package org.http4k.mcp.protocol.messages

import org.http4k.connect.model.MaxTokens
import org.http4k.connect.model.ModelName
import org.http4k.connect.model.Role
import org.http4k.connect.model.StopReason
import org.http4k.connect.model.SystemMessage
import org.http4k.connect.model.Temperature
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.ModelPreferences
import org.http4k.mcp.model.SamplingIncludeContext
import org.http4k.mcp.protocol.McpRpcMethod
import se.ansman.kotshi.JsonSerializable

object McpSampling : McpRpc {
    override val Method = McpRpcMethod.of("sampling/createMessage")

    @JsonSerializable
    data class Request(
        val messages: List<Message>,
        val maxTokens: MaxTokens,
        val systemPrompt: SystemMessage? = null,
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

