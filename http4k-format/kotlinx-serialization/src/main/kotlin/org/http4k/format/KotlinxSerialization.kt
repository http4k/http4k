package org.http4k.format

import kotlinx.serialization.json.JsonBuilder

private fun standardConfig(
    extraConfig: AutoMappingConfiguration<JsonBuilder>.() -> AutoMappingConfiguration<JsonBuilder>,
    extraBuilderConfig: (JsonBuilder.() -> JsonBuilder)
): JsonBuilder.() -> Unit = {
    ignoreUnknownKeys = true
    let(extraBuilderConfig)
        .asConfigurable()
        .withStandardMappings()
        .let(extraConfig)
        .done()
}

/**
 * To implement custom JSON configuration, create your own object singleton extending
 * ConfigurableKotlinxSerialization, passing in the JSON configuration block
 */
object KotlinxSerialization : ConfigurableKotlinxSerialization(standardConfig({this},{this})) {
    fun update(
        extraConfig: AutoMappingConfiguration<JsonBuilder>.() -> AutoMappingConfiguration<JsonBuilder>,
        extraBuilderConfig: (JsonBuilder.() -> JsonBuilder) = { this }
    ) = ConfigurableKotlinxSerialization(standardConfig(extraConfig, extraBuilderConfig))
}
