package org.http4k.connect.typesafe

import org.http4k.ai.model.ApiKey
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.typesafe.JevModels.JevLatest
import org.http4k.connect.typesafe.JevModels.JevPreview
import org.http4k.connect.typesafe.action.ModelCard
import org.http4k.routing.routes

class FakeTypeSafe(
    private val models: List<ModelCard> = DEFAULT_MODELS,
    private val answerer: QuestionAnswerer = FirstCriterionAnswerer
) : ChaoticHttpHandler() {

    override val app = routes(
        systemOne(answerer),
        getModels(models)
    )

    fun client() = TypeSafe.Http(ApiKey.of("fake"), this)

    companion object {
        val DEFAULT_MODELS = listOf(
            ModelCard(JevLatest, "The most recent stable release", "2025-11-04"),
            ModelCard(JevPreview, "The next release candidate", "2025-11-04")
        )
    }
}

fun main() {
    FakeTypeSafe().start()
}
