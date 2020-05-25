package org.http4k.format

import com.fasterxml.jackson.databind.ObjectMapper
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_YAML
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.ContentNegotiation.Companion.None
import org.http4k.lens.string
import org.http4k.websocket.WsMessage
import kotlin.reflect.KClass

open class ConfigurableJacksonYaml(val mapper: ObjectMapper) : AutoMarshalling() {
    override fun <T : Any> asA(input: String, target: KClass<T>): T = mapper.readValue(input, target.java)

    override fun asFormatString(input: Any): String = mapper.writeValueAsString(input)

    inline fun <reified T : Any> WsMessage.Companion.auto() = WsMessage.string().map(mapper.read<T>(), mapper.write())

    inline fun <reified T : Any> Body.Companion.auto(description: String? = null, contentNegotiation: ContentNegotiation = None) = autoBody<T>(description, contentNegotiation)

    inline fun <reified T : Any> autoBody(description: String? = null, contentNegotiation: ContentNegotiation = None): BiDiBodyLensSpec<T> = httpBodyLens(description, contentNegotiation, TEXT_YAML).map(mapper.read(), mapper.write())
}
