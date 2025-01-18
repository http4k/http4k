package org.http4k.mcp.model

/**
 *A selector used to choose the best model for a given request.
 */
interface ModelSelector {
    val name: ModelIdentifier
    fun score(model: ModelPreferences): Int

    companion object {
        /**
         * Anonymous implementation of the ModelSelector.
         */
        operator fun invoke(name: ModelIdentifier, score: (ModelPreferences) -> Int) = object : ModelSelector {
            override val name = name
            override fun score(model: ModelPreferences) = score(model)
        }
    }
}
