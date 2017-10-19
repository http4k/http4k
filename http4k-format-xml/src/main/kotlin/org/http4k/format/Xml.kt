package org.http4k.format

import com.google.gson.JsonElement
import org.http4k.asByteBuffer
import org.http4k.asString
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.BiDiLensSpec
import org.http4k.lens.BodyLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.ContentNegotiation.Companion.None
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta
import org.http4k.lens.root
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

    fun String.asXmlToJsonElement(): JsonElement = Gson.parse(XML.toJSONObject(this).toString())

    fun String.asXmlDocument(): Document =
        DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(this.byteInputStream())

    fun Document.asXmlString(): String = StringWriter().let {
        TransformerFactory.newInstance().newTransformer().transform(DOMSource(this), StreamResult(it))
        return it.toString()
    }

    fun <IN> BiDiLensSpec<IN, String>.xml() = this.map({ it.asXmlDocument() }, { it.asXmlString() })

    fun Body.Companion.xml(description: String? = null,
                           contentNegotiation: ContentNegotiation = ContentNegotiation.None): BiDiBodyLensSpec<Document> =
        root(listOf(Meta(true, "body", ParamMeta.ObjectParam, "body", description)), ContentType.APPLICATION_XML, contentNegotiation)
            .map(Body::payload, {it: ByteBuffer -> Body(it)})
            .map(ByteBuffer::asString, String::asByteBuffer).map({ it.asXmlDocument() }, { it.asXmlString() })

    inline fun <reified T : Any> Body.Companion.auto(description: String? = null, contentNegotiation: ContentNegotiation = None): BodyLensSpec<T> =
        root(listOf(Meta(true, "body", ParamMeta.ObjectParam, "body", description)), ContentType.APPLICATION_XML, contentNegotiation)
            .map({it.payload.asString()}, {it: String -> Body(it)})
            .map({ it.asA<T>() })
}
