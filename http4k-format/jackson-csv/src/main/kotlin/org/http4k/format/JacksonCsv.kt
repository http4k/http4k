package org.http4k.format

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

private fun standardConfig(
    extraAutoConfig: AutoMappingConfiguration<CsvMapper>.() -> AutoMappingConfiguration<CsvMapper>,
    extraConfig: CsvMapper.() -> CsvMapper
) = KotlinModule.Builder().build()
    .asConfigurable(CsvMapper())
    .withStandardMappings()
    .let(extraAutoConfig)
    .done()
    .deactivateDefaultTyping()
    .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(USE_BIG_DECIMAL_FOR_FLOATS, true)
    .configure(USE_BIG_INTEGER_FOR_INTS, true)
    .let { extraConfig(it as CsvMapper) }

object JacksonCsv : ConfigurableJacksonCsv(standardConfig({this},{this})) {
    fun update(
        extraAutoConfig: AutoMappingConfiguration<CsvMapper>.() -> AutoMappingConfiguration<CsvMapper>,
        extraConfig: CsvMapper.() -> CsvMapper = { this }
    ) = ConfigurableJacksonCsv(standardConfig(extraAutoConfig, extraConfig))
}
