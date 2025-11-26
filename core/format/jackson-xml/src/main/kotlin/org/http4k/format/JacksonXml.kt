package org.http4k.format

import tools.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import tools.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import tools.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import tools.jackson.module.kotlin.KotlinModule

/**
 * To implement custom XML configuration, create your own object singleton. Extra mappings can be added before done() is called.
 */
object JacksonXml : ConfigurableJacksonXml(
    KotlinModule.Builder().build().asConfigurableXml()
        .withStandardMappings()
        .done().apply {
            rebuild()
                .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(FAIL_ON_IGNORED_PROPERTIES, false)
                .configure(USE_BIG_DECIMAL_FOR_FLOATS, true)
                .configure(USE_BIG_INTEGER_FOR_INTS, true)
        }
)
