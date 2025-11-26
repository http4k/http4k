package org.http4k.format

import tools.jackson.core.StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION
import tools.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import tools.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
import tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import tools.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import tools.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

fun standardConfig(
    configFn: AutoMappingConfiguration<JsonMapper>.() -> AutoMappingConfiguration<JsonMapper>
) = KotlinModule.Builder().build()
    .asConfigurable(JsonMapper.builder().deactivateDefaultTyping().enable(INCLUDE_SOURCE_IN_LOCATION))
    .withStandardMappings()
    .let(configFn)
    .done()
    .rebuild()
    .configure(FAIL_ON_NULL_FOR_PRIMITIVES, true)
    .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(USE_BIG_DECIMAL_FOR_FLOATS, true)
    .configure(USE_BIG_INTEGER_FOR_INTS, true)
    .build()

/**
 * To implement custom JSON configuration, create your own object singleton. Extra mappings can be added before done() is called.
 */
object Jackson : ConfigurableJackson(standardConfig { this }) {
    fun custom(configFn: AutoMappingConfiguration<JsonMapper>.() -> AutoMappingConfiguration<JsonMapper>) =
        ConfigurableJackson(standardConfig(configFn))
}
