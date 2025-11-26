package org.http4k.format

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import tools.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import tools.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import tools.jackson.dataformat.csv.CsvMapper
import tools.jackson.module.kotlin.KotlinModule

object JacksonCsv : ConfigurableJacksonCsv(
    KotlinModule.Builder().build()
        .asConfigurable(CsvMapper.builder().deactivateDefaultTyping()
            .changeDefaultPropertyInclusion { inc -> inc.withValueInclusion(JsonInclude.Include.NON_NULL) })
        .withStandardMappings()
        .done()
        .rebuild()
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(FAIL_ON_IGNORED_PROPERTIES, false)
        .configure(USE_BIG_DECIMAL_FOR_FLOATS, true)
        .configure(USE_BIG_INTEGER_FOR_INTS, true)
        .build()
        as CsvMapper
)
