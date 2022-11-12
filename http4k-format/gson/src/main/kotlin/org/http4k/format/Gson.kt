package org.http4k.format

import com.google.gson.GsonBuilder

private fun standardConfig() = GsonBuilder()
    .serializeNulls()
    .asConfigurable()
    .withStandardMappings()

/**
 * To implement custom JSON configuration, create your own object singleton. Extra mappings can be added before done() is called.
 */
object Gson : ConfigurableGson(standardConfig().done()) {
    fun update(
        configureFn: AutoMappingConfiguration<GsonBuilder>.() -> AutoMappingConfiguration<GsonBuilder>
    ) = ConfigurableGson(standardConfig().let(configureFn).done())
}
