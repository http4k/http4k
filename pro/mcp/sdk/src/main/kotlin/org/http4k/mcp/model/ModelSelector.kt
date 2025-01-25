package org.http4k.mcp.model

import org.http4k.mcp.model.ModelScore.Companion.MAX

/**
 * A selector used to choose the best model for a given request.
 */
interface ModelSelector {
    val name: ModelIdentifier
    fun score(model: ModelPreferences): ModelScore

    companion object {
        /**
         * Anonymous implementation of the ModelSelector.
         */
        operator fun invoke(name: ModelIdentifier, score: (ModelPreferences) -> ModelScore = { MAX }) =
            object : ModelSelector {
                override val name = name
                override fun score(model: ModelPreferences) = score(model)
            }
    }
}
