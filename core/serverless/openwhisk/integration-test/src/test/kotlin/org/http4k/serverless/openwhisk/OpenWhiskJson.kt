package org.http4k.serverless.openwhisk

import com.fasterxml.jackson.annotation.JsonInclude
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import tools.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import tools.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import tools.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

object OpenWhiskJson : ConfigurableJackson(
    KotlinModule.Builder().build()
        .asConfigurable(JsonMapper.builder().deactivateDefaultTyping()
            .changeDefaultPropertyInclusion { inc -> inc.withValueInclusion(JsonInclude.Include.NON_NULL) })
        .withStandardMappings()
        .done()
        .rebuild()
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(FAIL_ON_IGNORED_PROPERTIES, false)
        .configure(USE_BIG_DECIMAL_FOR_FLOATS, true)
        .configure(USE_BIG_INTEGER_FOR_INTS, true)
        .build()
)
