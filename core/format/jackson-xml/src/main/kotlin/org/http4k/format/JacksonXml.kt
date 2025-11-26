package org.http4k.format

import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.KotlinModule

/**
 * To implement custom XML configuration, create your own object singleton. Extra mappings can be added before done() is called.
 */
object JacksonXml : ConfigurableJacksonXml(
    KotlinModule.Builder().build().asConfigurableXml()
        .withStandardMappings()
        .done().apply {
            rebuild()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
                .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
                .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
        }
)
