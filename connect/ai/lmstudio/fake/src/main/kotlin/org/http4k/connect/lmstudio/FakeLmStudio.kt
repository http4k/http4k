package org.http4k.connect.lmstudio

import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.lmstudio.action.Model
import org.http4k.connect.model.ModelName
import org.http4k.connect.storage.Storage
import org.http4k.routing.routes
import java.time.Clock
import java.time.Clock.systemUTC

class FakeLmStudio(
    val models: Storage<Model> = DEFAULT_MODELS,
    val completionGenerators: Map<ModelName, ChatCompletionGenerator> = emptyMap(),
    clock: Clock = systemUTC(),
) : ChaoticHttpHandler() {

    override val app = routes(
        getModels(models),
        chatCompletion(clock, completionGenerators),
        createEmbeddings(models)
    )

    /**
     * Convenience function to get client
     */
    fun client() = LmStudio.Http(this)
}

fun main() {
    FakeLmStudio().start()
}
