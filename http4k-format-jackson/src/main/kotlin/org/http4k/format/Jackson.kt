package org.http4k.format

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.lens.BiDiMapping

/**
 * To implement custom JSON configuration, copy and modify this file
 */

object Jackson : ConfigurableJackson(ConfigureJackson().withStandardMappings())

class ConfigureJackson : ConfigureAutoMarshallingJson<ObjectMapper> {
    private val module = KotlinModule()

    override fun <T> text(mapping: BiDiMapping<String, T>) = module.text(mapping)

    override fun done(): ObjectMapper = ObjectMapper()
        .registerModule(module)
        .disableDefaultTyping()
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(FAIL_ON_IGNORED_PROPERTIES, false)
        .configure(USE_BIG_DECIMAL_FOR_FLOATS, true)
        .configure(USE_BIG_INTEGER_FOR_INTS, true)
}
