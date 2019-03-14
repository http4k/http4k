package org.http4k.format

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import kotlin.reflect.KClass

open class ConfigurableJacksonXml(private val mapper: XmlMapper) : AutoMarshallingXml() {
    override fun Any.asXmlString() = mapper.writeValueAsString(this)

    override fun <T : Any> asA(input: String, target: KClass<T>): T = mapper.readValue(input, target.java)
}