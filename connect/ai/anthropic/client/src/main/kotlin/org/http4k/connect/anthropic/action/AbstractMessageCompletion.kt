package org.http4k.connect.anthropic.action

import org.http4k.connect.anthropic.Prompt
import org.http4k.connect.anthropic.ToolChoice
import org.http4k.connect.model.MaxTokens
import org.http4k.connect.model.ModelName
import org.http4k.connect.model.Temperature

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
