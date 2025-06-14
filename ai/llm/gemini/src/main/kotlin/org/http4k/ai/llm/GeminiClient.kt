package org.http4k.ai.llm

import org.http4k.ai.model.ApiKey
import org.http4k.client.JavaHttpClient
import org.http4k.connect.openai.OpenAI
import org.http4k.connect.openai.OpenAIAction
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ServerFilters

/**
 * Gemini integration
 */
class GeminiClient(private val apiKey: ApiKey, private val http: HttpHandler = JavaHttpClient()) :
    OpenAICompatibleClient {
    override fun invoke() = object : OpenAI {

        private val routedHttp = ClientFilters.BearerAuth(apiKey.value).then(http)

        override fun <R> invoke(action: OpenAIAction<R>) = action.toResult(
            routedHttp(
                action.toRequest()
                    .uri(Uri.of("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"))
            )
        )
    }
}
