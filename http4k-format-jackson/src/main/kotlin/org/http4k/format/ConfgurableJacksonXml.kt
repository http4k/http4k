package org.http4k.format

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import org.http4k.asString
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta
import org.http4k.lens.httpBodyRoot
import kotlin.reflect.KClass

open class ConfigurableJacksonXml(private val mapper: XmlMapper) {
    fun Any.asXmlString() = mapper.writeValueAsString(this)

    inline fun <reified T : Any> String.asA(): T = asA(this, T::class)

    fun <T : Any> asA(c: String, t: KClass<T>): T = mapper.readValue(c, t.java)

    inline fun <reified T : Any> Body.Companion.auto(description: String? = null, contentNegotiation: ContentNegotiation = ContentNegotiation.None): BiDiBodyLensSpec<T> =
        httpBodyRoot(listOf(Meta(true, "body", ParamMeta.ObjectParam, "body", description)), ContentType.APPLICATION_XML, contentNegotiation)
            .map({ it.payload.asString() }, { Body(it) })
            .map({ it.asA<T>() }, { it.asXmlString() })
}