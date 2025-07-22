package org.http4k.connect.openai

import org.http4k.ai.model.ApiKey
import org.http4k.ai.model.ModelName
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.defaultLocalUri
import org.http4k.chaos.start
import org.http4k.connect.openai.action.Model
import org.http4k.connect.storage.Storage
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters.BearerAuth
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Clock
import java.time.Clock.systemUTC

class FakeOpenAI(
    val models: Storage<Model> = DEFAULT_OPEN_AI_MODELS,
    val completionGenerators: Map<ModelName, ChatCompletionGenerator> = emptyMap(),
    clock: Clock = systemUTC(),
    baseUri: Uri = FakeOpenAI::class.defaultLocalUri
) : ChaoticHttpHandler() {

    override val app =
        routes(
            listOf(
                BearerAuth { true }
                    .then(
                        routes(
                            "/v1" bind openAIEndpoints(clock, baseUri, models, completionGenerators),
                            "/v1beta/openai" bind openAIEndpoints(clock, baseUri, models, completionGenerators)
                        )
                    ),
                serveGeneratedContent(),
            )
        )

    /**
     * Convenience function to get OpenAI client
     */
    fun client() = OpenAI.Http(ApiKey.of("openai-key"), this)
}

fun main() {
    FakeOpenAI().start()
}
