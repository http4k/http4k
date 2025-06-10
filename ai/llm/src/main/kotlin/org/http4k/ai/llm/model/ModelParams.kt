package org.http4k.ai.llm.model

import org.http4k.ai.llm.chat.ChatResponseFormat
import org.http4k.ai.llm.chat.ToolSelection
import org.http4k.ai.llm.tools.LLMTool
import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.Temperature

data class ModelParams(
    val modelName: ModelName,
    val temperature: Temperature? = null,
    val stopSequences: List<String> = emptyList(),
    val maxOutputTokens: MaxTokens? = null,
    val responseFormat: ChatResponseFormat? = null,
    val tools: List<LLMTool> = emptyList(),
    val toolSelection: ToolSelection? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val frequencyPenalty: Double? = null,
    val presencePenalty: Double? = null,
)
