package org.http4k.ai.model

import org.http4k.ai.chat.ResponseFormat
import org.http4k.ai.tools.ToolChoice
import org.http4k.ai.tools.Tool

data class ModelParams(
    val modelName: ModelName? = null,
    val temperature: Temperature? = null,
    val stopSequences: List<String> = emptyList(),
    val maxOutputTokens: MaxTokens? = null,
    val responseFormat: ResponseFormat? = null,
    val tools: List<Tool> = emptyList(),
    val toolChoice: ToolChoice? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val frequencyPenalty: Double? = null,
    val presencePenalty: Double? = null,
)
