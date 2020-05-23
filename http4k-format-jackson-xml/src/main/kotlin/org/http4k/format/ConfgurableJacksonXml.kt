package org.http4k.format

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlin.reflect.KClass

open class ConfigurableJacksonXml(private val mapper: XmlMapper) : AutoMarshallingXml() {
    override fun Any.asXmlString(): String = mapper.writeValueAsString(this)

    override fun <T : Any> asA(input: String, target: KClass<T>): T = mapper.readValue(input, target.java)
}

fun KotlinModule.asConfigurableXml() = asConfigurable(
    XmlMapper(JacksonXmlModule().apply { setDefaultUseWrapper(false) })
)
