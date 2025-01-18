package org.http4k.connect.mcp

import org.http4k.connect.mcp.model.MaxTokens
import org.http4k.connect.mcp.model.Meta
import org.http4k.connect.mcp.model.ModelPreferences
import org.http4k.connect.mcp.model.Role
import org.http4k.connect.mcp.model.Temperature
import org.http4k.mcp.prompts.Content

object McpSampling {
    data class Message(val role: Role, val content: Content) {
        object Create : HasMethod {
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
        }
    }

}
