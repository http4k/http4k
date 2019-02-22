package org.http4k.format

import com.google.gson.JsonElement
import org.http4k.asByteBuffer
import org.http4k.asString
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.lens.*
import org.http4k.lens.ContentNegotiation.Companion.None
import org.json.XML
import org.w3c.dom.Document
import java.io.StringWriter
import java.nio.ByteBuffer
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object Xml {

    inline fun <reified T : Any> String.asA(): T = Gson.asA(asXmlToJsonElement(), T::class)

    fun String.asXmlToJsonElement(): JsonElement = Gson.parse(XML.toJSONObject(this, true).toString())

    fun String.asXmlDocument(): Document =
            DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(this.byteInputStream())

    fun Document.asXmlString(): String = StringWriter().let {
        TransformerFactory.newInstance().newTransformer().transform(DOMSource(this), StreamResult(it))
        it.toString()
    }

    fun <IN : Any> BiDiLensSpec<IN, String>.xml() = this.map({ it.asXmlDocument() }, { it.asXmlString() })

    fun Body.Companion.xml(description: String? = null,
                           contentNegotiation: ContentNegotiation = ContentNegotiation.None): BiDiBodyLensSpec<Document> =
            httpBodyRoot(listOf(Meta(true, "body", ParamMeta.ObjectParam, "body", description)), ContentType.APPLICATION_XML, contentNegotiation)
                    .map(Body::payload) { Body(it) }
                    .map(ByteBuffer::asString, String::asByteBuffer).map({ it.asXmlDocument() }, { it.asXmlString() })

    inline fun <reified T : Any> Body.Companion.auto(description: String? = null, contentNegotiation: ContentNegotiation = None): BodyLensSpec<T> =
            httpBodyRoot(listOf(Meta(true, "body", ParamMeta.ObjectParam, "body", description)), ContentType.APPLICATION_XML, contentNegotiation)
                    .map({ it.payload.asString() }, { Body(it) })
                    .map { it.asA<T>() }
}
