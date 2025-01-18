package org.http4k.mcp.protocol

import org.http4k.mcp.model.Content
import org.http4k.mcp.model.MaxTokens
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.ModelPreferences
import org.http4k.mcp.model.Role
import org.http4k.mcp.model.Temperature

object McpSampling : HasMethod {
    override val Method = McpRpcMethod.of("sampling/create_message")

    data class Request(
        val messages: List<Message>,
        val maxTokens: MaxTokens,
        val systemPrompt: String? = null,
        val includeContext: IncludeContext? = null,
        val temperature: Temperature? = null,
        val stopSequences: List<String>? = null,
        val modelPreferences: ModelPreferences? = null,
        val metadata: Map<String, Any> = emptyMap(),
        override val _meta: Meta = HasMeta.default
    ) : ServerMessage.Request, HasMeta {
        enum class IncludeContext { none, thisServer, allServers }
    }

    data class Response(
        val model: String,
        val stopReason: String,
        val role: Role,
        val content: Content
    ) : ClientMessage.Response
}
