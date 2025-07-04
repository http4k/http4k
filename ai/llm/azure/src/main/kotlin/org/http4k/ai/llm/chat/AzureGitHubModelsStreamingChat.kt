package org.http4k.ai.llm.chat

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.ai.llm.AzureGitHubModelsClient
import org.http4k.ai.llm.LLMError.Http
import org.http4k.ai.model.ApiKey
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Response

/**
 * Azure GitHub Models Streaming Chat interface
 */
fun StreamingChat.Companion.AzureGitHubModels(
    apiKey: ApiKey,
    http: HttpHandler = JavaHttpClient()
) =
    object : StreamingChat {
        private val client = AzureGitHubModelsClient(apiKey, http)()

        override fun invoke(request: ChatRequest) = client(request.toOpenAI(true))
            .map { it.map { it.toHttp4k() } }
            .mapFailure { Http(Response(it.status).body(it.message ?: "")) }
    }
