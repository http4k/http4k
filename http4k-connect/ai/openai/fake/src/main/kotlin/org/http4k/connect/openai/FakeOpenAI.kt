package org.http4k.connect.openai

import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.defaultLocalUri
import org.http4k.chaos.start
import org.http4k.client.JavaHttpClient
import org.http4k.connect.model.ModelName
import org.http4k.connect.openai.action.Model
import org.http4k.connect.openai.plugins.PluginIntegration
import org.http4k.connect.storage.Storage
import org.http4k.contract.ui.swagger.swaggerUiWebjar
import org.http4k.core.HttpHandler
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
    baseUri: Uri = FakeOpenAI::class.defaultLocalUri,
    http: HttpHandler = JavaHttpClient(),
    vararg plugins: PluginIntegration
) : ChaoticHttpHandler() {

    override val app =
        routes(
            *(
                plugins
                    .map { it.buildIntegration(baseUri, http, clock) }
                    .flatMap {
                        listOf(
                            it.filter.then(
                                "/${it.pluginId}" bind swaggerUiWebjar {
                                    pageTitle = "OpenAI plugin " + it.pluginId
                                    url = baseUri.path("/${it.pluginId}/openapi.json").toString()
                                }
                            ),
                            it.httpHandler
                        )
                    } +
                    listOf(
                        BearerAuth { true }
                            .then(
                                routes(
                                    getModels(models),
                                    chatCompletion(clock, completionGenerators),
                                    createEmbeddings(models),
                                    generateImage(clock, baseUri),
                                )
                            ),
                        serveGeneratedContent(),
                        index(plugins.map { it.pluginId })
                    )
                ).toTypedArray()
        )

    /**
     * Convenience function to get OpenAI client
     */
    fun client() = OpenAI.Http(OpenAIToken.of("openai-key"), this)
}

fun main() {
    FakeOpenAI().start()
}
