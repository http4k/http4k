package org.http4k.format

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.reflect.KClass

open class ConfigurableJacksonYaml(private val mapper: ObjectMapper) : AutoMarshalling() {
    override fun <T : Any> asA(input: String, target: KClass<T>): T = mapper.readValue(input, target.java)

    override fun asString(input: Any): String = mapper.writeValueAsString(input)
}
