package org.http4k.connect.anthropic.action

import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.Prompt
import org.http4k.ai.model.Temperature
import org.http4k.connect.anthropic.ToolChoice

sealed interface AbstractMessageCompletion {
    val model: ModelName
    val messages: List<Message>
    val max_tokens: MaxTokens
    val metadata: Metadata?
    val stop_sequences: List<String>
    val system: Prompt?
    val temperature: Temperature?
    val tool_choice: ToolChoice?
    val tools: List<Tool>
    val top_k: Int?
    val top_p: Double?
    val stream: Boolean
}

sealed interface GeneratedContent
