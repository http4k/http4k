package org.http4k.format

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.asString
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.HttpMessage
import org.http4k.core.with
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.lens.httpBodyRoot
import java.io.InputStream
import kotlin.reflect.KClass

open class ConfigurableJacksonXml(
    private val mapper: XmlMapper,
    override val defaultContentType: ContentType = APPLICATION_XML
) : AutoMarshallingXml() {
    override fun Any.asXmlString(): String = mapper.writeValueAsString(this)

    override fun <T : Any> asA(input: String, target: KClass<T>): T = mapper.readValue(input, target.java)
    override fun <T : Any> asA(input: InputStream, target: KClass<T>): T = mapper.readValue(input, target.java)

    override fun asInputStream(input: Any): InputStream = mapper.writeValueAsBytes(input).inputStream()

    /**
     * Convenience function to write the object as XM to the message body and set the content type.
     */
    inline fun <reified T : Any, R : HttpMessage> R.xml(t: T): R = with<R>(Body.auto<T>().toLens() of t)

    /**
     * Convenience function to read an object as XML from the message body.
     */
    inline fun <reified T: Any> HttpMessage.xml(): T = Body.auto<T>().toLens()(this)

    inline fun <reified T : Any> autoBody(
        description: String? = null,
        contentNegotiation: ContentNegotiation = ContentNegotiation.None,
        contentType: ContentType = defaultContentType
    ): BiDiBodyLensSpec<T> =
        httpBodyRoot(
            listOf(Meta(true, "body", ObjectParam, "body", description, emptyMap())),
            contentType,
            contentNegotiation
        )
            .map({ it.payload.asString() }, { Body(it) })
            .map({ it.asA<T>() }, { it.asXmlString() })
}

fun KotlinModule.asConfigurableXml() = asConfigurable(
    XmlMapper(JacksonXmlModule().apply { setDefaultUseWrapper(false) })
)

inline operator fun <reified T : Any> ConfigurableJacksonXml.invoke(msg: HttpMessage): T = autoBody<T>().toLens()(msg)
inline operator fun <reified T : Any, R : HttpMessage> ConfigurableJacksonXml.invoke(item: T) = autoBody<T>().toLens().of<R>(item)
