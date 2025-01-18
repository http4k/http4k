package org.http4k.mcp.model

/**
 *A selector used to choose the best model for a given request.
 */
interface ModelSelector {
    val name: ModelName
    fun score(model: ModelPreferences): Int

    companion object {
        /**
         * Anaoymous implementation of the ModelSelector.
         */
        operator fun invoke(name: ModelName, score: (ModelPreferences) -> Int) = object : ModelSelector {
            override val name = name
            override fun score(model: ModelPreferences) = score(model)
        }
    }
}
