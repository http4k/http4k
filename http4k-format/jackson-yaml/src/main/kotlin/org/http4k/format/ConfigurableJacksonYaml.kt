package org.http4k.format

import com.fasterxml.jackson.databind.ObjectMapper
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.TEXT_YAML
import org.http4k.core.HttpMessage
import org.http4k.core.with
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.ContentNegotiation.Companion.None
import org.http4k.lens.string
import org.http4k.websocket.WsMessage
import java.io.InputStream
import kotlin.reflect.KClass

open class ConfigurableJacksonYaml(val mapper: ObjectMapper, override val defaultContentType: ContentType = TEXT_YAML) :
    AutoMarshalling() {

    override fun <T : Any> asA(input: String, target: KClass<T>): T = mapper.readValue(input, target.java)
    override fun <T : Any> asA(input: InputStream, target: KClass<T>): T = mapper.readValue(input, target.java)

    override fun asFormatString(input: Any): String = mapper.writeValueAsString(input)

    override fun asInputStream(input: Any): InputStream = mapper.writeValueAsBytes(input).inputStream()

    inline fun <reified T : Any> WsMessage.Companion.auto() = WsMessage.string().map(mapper.read<T>(), mapper.write())

    inline fun <reified T : Any> Body.Companion.auto(
        description: String? = null,
        contentNegotiation: ContentNegotiation = None
    ) = autoBody<T>(description, contentNegotiation)

    inline fun <reified T : Any> autoBody(
        description: String? = null,
        contentNegotiation: ContentNegotiation = None
    ): BiDiBodyLensSpec<T> =
        httpBodyLens(description, contentNegotiation, defaultContentType).map(mapper.read(), mapper.write())

    /**
     * Convenience function to write the object as YAML to the message body and set the content type.
     */
    inline fun <reified T : Any, R : HttpMessage> R.yaml(t: T): R = with(Body.auto<T>().toLens() of t)

    /**
     * Convenience function to read an object as YAML from the message body.
     */
    inline fun <reified T: Any> HttpMessage.yaml(): T = Body.auto<T>().toLens()(this)
}

inline operator fun <reified T : Any> ConfigurableJacksonYaml.invoke(msg: HttpMessage): T = autoBody<T>().toLens()(msg)
inline operator fun <reified T : Any, R : HttpMessage> ConfigurableJacksonYaml.invoke(item: T) = autoBody<T>().toLens().of<R>(item)
