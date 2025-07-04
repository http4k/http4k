package org.http4k.ai.llm

import org.http4k.ai.model.ApiKey
import org.http4k.ai.model.ModelName
import org.http4k.client.JavaHttpClient
import org.http4k.connect.openai.OpenAI
import org.http4k.connect.openai.OpenAIAction
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.BearerAuth

/**
 * GitHubModels integration
 */
class GitHubModelsClient(
    private val apiKey: ApiKey,
    private val http: HttpHandler = JavaHttpClient(),
    private val githubOrg: String? = null
) : OpenAICompatibleClient {
    override fun invoke() = object : OpenAI {
        private val orgPath = githubOrg?.let { "orgs/$it/" } ?: ""
        private val routedHttp = BearerAuth(apiKey.value).then(http)

        override fun <R> invoke(action: OpenAIAction<R>) = action.toResult(
            routedHttp(
                action.toRequest()
                    .uri(Uri.of("https://models.github.ai/${orgPath}inference/chat/completions"))
            )
        )
    }

    object Models {
        val OpenAI_GPT4 = ModelName.of("openai/gpt-4.1")
        val OpenAI_O1 = ModelName.of("openai/o1")
    }
}
