package org.http4k.ai.llm.chat

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.ai.llm.LLMError.Http
import org.http4k.ai.llm.OpenAIApi
import org.http4k.ai.llm.OpenAICompatibleClient
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Response

/**
 * OpenAI Chat interface for using with the real OpenAI API endpoints
 */
fun StreamingChat.Companion.OpenAI(apiKey: OpenAIApi.ApiKey, http: HttpHandler = JavaHttpClient(), org: OpenAIApi.Org? = null) =
    OpenAI(OpenAIApi(apiKey, http, org))

/**
 * Chat interface for any OpenAI-compatible client
 */
fun StreamingChat.Companion.OpenAI(openAICompatibleClient: OpenAICompatibleClient) = object : StreamingChat {
    private val client = openAICompatibleClient()

    override fun invoke(request: ChatRequest) = client(request.toOpenAI(true))
        .map { it.map { it.toHttp4k() } }
        .mapFailure { Http(Response(it.status).body(it.message ?: "")) }
}
