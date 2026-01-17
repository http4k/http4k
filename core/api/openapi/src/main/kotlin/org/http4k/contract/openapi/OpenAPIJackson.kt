package org.http4k.contract.openapi

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import tools.jackson.core.StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

private fun standardConfig(
    configFn: AutoMappingConfiguration<JsonMapper>.() -> AutoMappingConfiguration<JsonMapper>
) = KotlinModule.Builder().build()
    .asConfigurable(
        JsonMapper.builder().enable(INCLUDE_SOURCE_IN_LOCATION).deactivateDefaultTyping()
            .changeDefaultPropertyInclusion {
                it
                    .withContentInclusion(NON_NULL)
                    .withValueInclusion(NON_NULL)
            })
    .withStandardMappings()
    .let(configFn)
    .done()
    .rebuild()
    .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
    .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
    .build()

object OpenAPIJackson : ConfigurableJackson(standardConfig { this }) {
    fun custom(configFn: AutoMappingConfiguration<JsonMapper>.() -> AutoMappingConfiguration<JsonMapper>) =
        ConfigurableJackson(standardConfig(configFn))
}
