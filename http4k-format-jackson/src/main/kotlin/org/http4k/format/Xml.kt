package org.http4k.format

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

//    .disableDefaultTyping()
//    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
//    .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
//    .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
//    .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)

object Xml : ConfigurableXml(KotlinModule().asConfigurableXml()
    .withStandardMappings()
    .done()
    .disableDefaultTyping()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
    .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true) as XmlMapper
)