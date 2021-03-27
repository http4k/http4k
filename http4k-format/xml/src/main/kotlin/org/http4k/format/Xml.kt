package org.http4k.format

import com.google.gson.JsonElement
import org.http4k.asByteBuffer
import org.http4k.asString
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.BiDiLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta
import org.http4k.lens.httpBodyRoot
import org.json.XML
import org.w3c.dom.Document
import java.io.InputStream
import java.io.StringWriter
import java.nio.ByteBuffer
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.reflect.KClass

object Xml : AutoMarshallingXml() {

    override fun Any.asXmlString(): String = throw UnsupportedOperationException("")

    override fun <T : Any> asA(input: String, target: KClass<T>): T = Gson.asA(input.asXmlToJsonElement(), target)

    override fun <T : Any> asA(input: InputStream, target: KClass<T>): T = Gson.asA(input.reader().readText().asXmlToJsonElement(), target)

    fun String.asXmlToJsonElement(): JsonElement = Gson.parse(XML.toJSONObject(this, true).toString())

    @JvmName("stringAsXmlToJsonElement")
    fun asXmlToJsonElement(input: String): JsonElement = input.asXmlToJsonElement()

    fun String.asXmlDocument(): Document =
        DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(byteInputStream())

    fun Document.asXmlString(): String = StringWriter().let {
        TransformerFactory.newInstance().newTransformer().transform(DOMSource(this), StreamResult(it))
        it.toString()
    }

    fun <IN : Any> BiDiLensSpec<IN, String>.xml() = map({ it.asXmlDocument() }, { it.asXmlString() })

    fun Body.Companion.xml(description: String? = null,
                           contentNegotiation: ContentNegotiation = ContentNegotiation.None): BiDiBodyLensSpec<Document> =
        httpBodyRoot(listOf(Meta(true, "body", ParamMeta.ObjectParam, "body", description)), APPLICATION_XML, contentNegotiation)
            .map(Body::payload) { Body(it) }
            .map(ByteBuffer::asString, String::asByteBuffer).map({ it.asXmlDocument() }, { it.asXmlString() })
}
