package org.http4k.connect.mcp

object Sampling {
    data class Message(
        val role: Role,
        val content: Prompt.Content,
    ) {
        object Create : HasMethod {
            override val method = McpRpcMethod.of("sampling/create_message")

            data class Request(
                val messages: List<Message>,
                val maxTokens: Int,
                val systemPrompt: String? = null,
                val includeContext: IncludeContext? = null,
                val temperature: Double? = null,
                val stopSequences: List<String>? = null,
                val modelPreferences: ModelPreferences? = null,
                val metadata: Map<String, Any> = emptyMap(),
                override val _meta: Meta = HasMeta.default
            ) : ServerResponse, HasMeta {
                enum class IncludeContext { none, thisServer, allServers }
            }
        }
    }

}
