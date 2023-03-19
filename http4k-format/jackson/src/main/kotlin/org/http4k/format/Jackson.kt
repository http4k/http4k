package org.http4k.format

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

private fun standardConfig(
    configFn: AutoMappingConfiguration<ObjectMapper>.() -> AutoMappingConfiguration<ObjectMapper>
) = KotlinModule.Builder().build()
    .asConfigurable()
    .withStandardMappings()
    .let(configFn)
    .done()
    .deactivateDefaultTyping()
    .configure(FAIL_ON_NULL_FOR_PRIMITIVES, true)
    .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(USE_BIG_DECIMAL_FOR_FLOATS, true)
    .configure(USE_BIG_INTEGER_FOR_INTS, true)

/**
 * To implement custom JSON configuration, create your own object singleton. Extra mappings can be added before done() is called.
 */
object Jackson : ConfigurableJackson(standardConfig { this }) {
    fun custom(configFn: AutoMappingConfiguration<ObjectMapper>.() -> AutoMappingConfiguration<ObjectMapper>) =
        ConfigurableJackson(standardConfig(configFn))
}
