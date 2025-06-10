package org.http4k.ai.llm.chat

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.ai.llm.LLMError.Http
import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.SystemPrompt
import org.http4k.connect.anthropic.AnthropicAI
import org.http4k.connect.anthropic.AnthropicIApiKey
import org.http4k.connect.anthropic.ApiVersion
import org.http4k.connect.anthropic.ApiVersion.Companion._2023_06_01
import org.http4k.connect.anthropic.Http
import org.http4k.connect.anthropic.action.MessageCompletion
import org.http4k.connect.anthropic.action.Metadata
import org.http4k.core.HttpHandler
import org.http4k.core.Response

fun Chat.Companion.AnthropicAI(
    apiKey: AnthropicIApiKey,
    http: HttpHandler,
    apiVersion: ApiVersion = _2023_06_01,
    metadata: Metadata? = null,
    systemPrompt: SystemPrompt? = null,
) = object : Chat {
    private val client = AnthropicAI.Http(apiKey, apiVersion, http)

    override fun invoke(request: ChatRequest) =
        client(
            MessageCompletion(
                request.params.modelName,
                request.messages.map { it.toAnthropic() },
                request.params.maxOutputTokens ?: MaxTokens.of(64000),
                metadata,
                request.params.stopSequences,
                systemPrompt,
                request.params.temperature,
                request.params.toolSelection?.toLLM(),
                request.params.tools.map { it.toAnthropic() },
                request.params.topK,
                request.params.topP
            )
        )
            .map { ChatResponse(it.content.toLLM(), it.toMetadata()) }
            .mapFailure { Http(Response(it.status).body(it.message ?: "")) }
}

