package org.http4k.format

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.asString
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta
import org.http4k.lens.httpBodyRoot
import java.io.InputStream
import kotlin.reflect.KClass

open class ConfigurableJacksonXml(private val mapper: XmlMapper, val defaultContentType: ContentType = ContentType.APPLICATION_XML) : AutoMarshallingXml() {
    override fun Any.asXmlString(): String = mapper.writeValueAsString(this)

    override fun <T : Any> asA(input: String, target: KClass<T>): T = mapper.readValue(input, target.java)
    override fun <T : Any> asA(input: InputStream, target: KClass<T>): T = mapper.readValue(input, target.java)

    inline fun <reified T : Any> autoBody(
        description: String? = null,
        contentNegotiation: ContentNegotiation = ContentNegotiation.None,
        contentType: ContentType = defaultContentType
    ): BiDiBodyLensSpec<T> =
        httpBodyRoot(listOf(Meta(true, "body", ParamMeta.ObjectParam, "body", description)), contentType, contentNegotiation)
            .map({ it.payload.asString() }, { Body(it) })
            .map({ it.asA<T>() }, { it.asXmlString() })
}

fun KotlinModule.asConfigurableXml() = asConfigurable(
    XmlMapper(JacksonXmlModule().apply { setDefaultUseWrapper(false) })
)
