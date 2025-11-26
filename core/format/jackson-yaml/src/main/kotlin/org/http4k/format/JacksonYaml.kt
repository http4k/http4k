package org.http4k.format

import tools.jackson.core.StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION
import tools.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import tools.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import tools.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.dataformat.yaml.YAMLWriteFeature.WRITE_DOC_START_MARKER
import tools.jackson.module.kotlin.KotlinModule

private fun standardConfigYaml(
    configFn: AutoMappingConfiguration<YAMLMapper>.() -> AutoMappingConfiguration<YAMLMapper>,
) = KotlinModule.Builder().build()
    .asConfigurable(
        YAMLMapper.builder().deactivateDefaultTyping()
            .enable(INCLUDE_SOURCE_IN_LOCATION)
            .disable(WRITE_DOC_START_MARKER)
    )
    .withStandardMappings()
    .let(configFn)
    .done()
    .rebuild()
    .configure(FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(USE_BIG_DECIMAL_FOR_FLOATS, true)
    .configure(USE_BIG_INTEGER_FOR_INTS, true)
    .build()

object JacksonYaml : ConfigurableJacksonYaml(standardConfigYaml { this }) {
    fun custom(configFn: AutoMappingConfiguration<YAMLMapper>.() -> AutoMappingConfiguration<YAMLMapper>) =
        ConfigurableJacksonYaml(standardConfigYaml(configFn))
}
