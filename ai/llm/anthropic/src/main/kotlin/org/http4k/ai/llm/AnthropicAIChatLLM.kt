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
import org.http4k.connect.anthropic.action.Content
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
    override fun invoke(request: ChatRequest) =
        with(request) {
            request.messages
            client.messageCompletion(
                params.modelName,
                UserPrompt.of(""),
                params.maxOutputTokens ?: MaxTokens.of(MAX_VALUE),
                metadata,
                params.stopSequences,
                systemPrompt,
                params.temperature,
                params.toolSelection?.toLLM(),
                params.tools.map { it.toAnthropic() },
                params.topK,
                params.topP
            )
                .map {
                    it.content.map {
                        it.toLLM()
                    }
                    TODO()
                }
                .mapFailure { Http(Response(it.status).body(it.message ?: "")) }

        }
}

private fun Content.toLLM(): org.http4k.ai.llm.model.Message {
    org.http4k.ai.llm.model.Message.Assistant()
    TODO("Not yet implemented")
}

private fun ToolSelection.toLLM() = when (this) {
    Auto -> ToolChoice.Auto(true)
    Required -> ToolChoice.Any(true)
}

private fun LLMTool.toAnthropic() = Tool(name, description, inputSchema, null, null)
