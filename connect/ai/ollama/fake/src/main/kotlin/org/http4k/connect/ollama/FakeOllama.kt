package org.http4k.connect.ollama

import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.model.ModelName
import org.http4k.connect.ollama.action.Model
import org.http4k.connect.storage.Storage
import org.http4k.routing.routes
import java.time.Clock
import java.time.Clock.systemUTC

class FakeOllama(
    val models: Storage<Model> = DEFAULT_OLLAMA_MODELS,
    val completionGenerators: Map<ModelName, ChatCompletionGenerator> = emptyMap(),
    clock: Clock = systemUTC(),
) : ChaoticHttpHandler() {

    override val app =
        routes(
            getModels(models),
            completion(clock, completionGenerators),
            pullModel(),
            chatCompletion(clock, completionGenerators),
            createEmbeddings(models)
        )

    /**
     * Convenience function to get Ollama client
     */
    fun client() = Ollama.Http(this)
}

fun main() {
    FakeOllama().start()
}
