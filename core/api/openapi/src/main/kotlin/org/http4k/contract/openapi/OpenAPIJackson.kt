package org.http4k.contract.openapi

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings

private fun standardConfig(
    configFn: AutoMappingConfiguration<ObjectMapper>.() -> AutoMappingConfiguration<ObjectMapper>
) = KotlinModule.Builder().build()
    .asConfigurable()
    .withStandardMappings()
    .let(configFn)
    .done()
    .deactivateDefaultTyping()
    .setSerializationInclusion(NON_NULL)
    .configure(FAIL_ON_NULL_FOR_PRIMITIVES, true)
    .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(USE_BIG_DECIMAL_FOR_FLOATS, true)
    .configure(USE_BIG_INTEGER_FOR_INTS, true)

object OpenAPIJackson : ConfigurableJackson(standardConfig { this }) {
    fun custom(configFn: AutoMappingConfiguration<ObjectMapper>.() -> AutoMappingConfiguration<ObjectMapper>) =
        ConfigurableJackson(standardConfig(configFn))
}
