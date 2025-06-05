package org.http4k.ai.llm

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.ai.llm.LLMError.Http
import org.http4k.ai.llm.chat.Chat
import org.http4k.ai.llm.chat.ChatRequest
import org.http4k.ai.llm.model.ToolSelection
import org.http4k.ai.llm.model.ToolSelection.Auto
import org.http4k.ai.llm.model.ToolSelection.Required
import org.http4k.ai.llm.tools.LLMTool
import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.SystemPrompt
import org.http4k.ai.model.UserPrompt
import org.http4k.connect.anthropic.AnthropicAI
import org.http4k.connect.anthropic.AnthropicIApiKey
import org.http4k.connect.anthropic.ApiVersion
import org.http4k.connect.anthropic.Http
import org.http4k.connect.anthropic.ToolChoice
import org.http4k.connect.anthropic.action.Metadata
import org.http4k.connect.anthropic.action.Tool
import org.http4k.connect.anthropic.messageCompletion
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import java.lang.Integer.MAX_VALUE

fun AnthropicAIChatLLM(
    apiKey: AnthropicIApiKey,
    http: HttpHandler,
    apiVersion: ApiVersion,
    metadata: Metadata? = null,
    systemPrompt: SystemPrompt? = null,
) = object : Chat {
    private val client = AnthropicAI.Http(apiKey, apiVersion, http)
    override fun invoke(request: ChatRequest) = client.messageCompletion(
        request.params.modelName,
        UserPrompt.of(""),
        request.params.maxOutputTokens ?: MaxTokens.of(MAX_VALUE),
        metadata,
        request.params.stopSequences,
        systemPrompt,
        request.params.temperature,
        request.params.toolSelection?.toLLM(),
        request.params.tools.map { it.toAnthropic() },
        request.params.topK,
        request.params.topP
    )
        .map {
            TODO()
        }
        .mapFailure { Http(Response(it.status).body(it.message ?: "")) }
}

private fun ToolSelection.toLLM() = when (this) {
    Auto -> ToolChoice.Auto(true)
    Required -> ToolChoice.Any(true)
}

private fun LLMTool.toAnthropic() = Tool(name, description, inputSchema, null, null)
