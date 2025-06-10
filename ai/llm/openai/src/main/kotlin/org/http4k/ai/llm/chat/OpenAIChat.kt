package org.http4k.ai.llm.chat

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.ai.llm.LLMError.Http
import org.http4k.ai.llm.OpenAIApi
import org.http4k.ai.llm.OpenAICompatibleClient
import org.http4k.ai.model.TokenUsage
import org.http4k.core.HttpHandler
import org.http4k.core.Response

/**
 * OpenAI Chat interface for using with the real OpenAI API endpoints
 */
fun Chat.Companion.OpenAI(apiKey: OpenAIApi.ApiKey, http: HttpHandler, org: OpenAIApi.Org? = null) =
    OpenAI(OpenAIApi(apiKey, http, org))

/**
 * Chat interface for any OpenAI-compatible client
 */
fun Chat.Companion.OpenAI(openAICompatibleClient: OpenAICompatibleClient) = object : Chat {
    private val client = openAICompatibleClient()

    override fun invoke(request: ChatRequest) =
        client(request.asOpenAI(false))
            .map { it.first() }
            .map {
                ChatResponse(
                    it.toLLM(),
                    ChatResponse.Metadata(
                        it.id, it.model, it.usage
                            ?.let { TokenUsage(it.prompt_tokens, it.completion_tokens) })
                )
            }
            .mapFailure { Http(Response(it.status).body(it.message ?: "")) }
}
