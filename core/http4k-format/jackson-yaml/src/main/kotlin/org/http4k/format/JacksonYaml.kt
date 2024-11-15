package org.http4k.format

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER
import com.fasterxml.jackson.module.kotlin.KotlinModule

private fun standardConfig(
    configFn: AutoMappingConfiguration<ObjectMapper>.() -> AutoMappingConfiguration<ObjectMapper>,
) = KotlinModule.Builder().build()
    .asConfigurable(ObjectMapper(YAMLFactory().disable(WRITE_DOC_START_MARKER)))
    .withStandardMappings()
    .let(configFn)
    .done()
    .deactivateDefaultTyping()
    .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(USE_BIG_DECIMAL_FOR_FLOATS, true)
    .configure(USE_BIG_INTEGER_FOR_INTS, true)

object JacksonYaml : ConfigurableJacksonYaml(standardConfig { this }) {
    fun custom(configFn: AutoMappingConfiguration<ObjectMapper>.() -> AutoMappingConfiguration<ObjectMapper>) =
        ConfigurableJacksonYaml(standardConfig(configFn))
}
