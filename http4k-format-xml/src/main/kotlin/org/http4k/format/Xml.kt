package org.http4k.format

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
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
import org.w3c.dom.Node
import java.io.StringWriter
import java.nio.ByteBuffer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

open class ConfigurableJacksonXml(val mapper: XmlMapper) {

    inline fun <reified T : Any> String.asA(): T = mapper.convertValue(mapper.readTree(this).clean(), T::class.java)

    fun String.asXmlNode(): Node = mapper.convertValue(this, Node::class.java)

    fun Node.asXmlString(): String {
        val transformer = TransformerFactory.newInstance().newTransformer()
        val writer = StringWriter()
        transformer.transform(DOMSource(this), StreamResult(writer))
        return writer.toString()
    }

    fun <IN> BiDiLensSpec<IN, String, String>.xml() = this.map({ it.asXmlNode() }, { it.asXmlString() })

    fun Body.Companion.xml(description: String? = null,
                           contentNegotiation: ContentNegotiation = ContentNegotiation.None): BiDiBodyLensSpec<Node> =
        baseBody(description, contentNegotiation).map({ it.asXmlNode() }, { it.asXmlString() })

    inline fun <reified T : Any> Body.Companion.auto(description: String? = null, contentNegotiation: ContentNegotiation = None): BodyLensSpec<T> =
        baseBody(description, contentNegotiation).map({ it.asA<T>() })


}

fun JsonNode.clean(): JsonNode {
    if(this.isObject) {
        val objectNode = this as ObjectNode
        objectNode.remove("")?.let {
            objectNode.set("_textValue", it)
        }
    }
    this.elements().forEach { it.clean() }
    return this
}

fun baseBody(description: String?, contentNegotiation: ContentNegotiation): BiDiBodyLensSpec<String> = root(listOf(Meta(true, "body", ParamMeta.ObjectParam, "body", description)), ContentType.APPLICATION_XML, contentNegotiation)
    .map(ByteBuffer::asString, String::asByteBuffer)

object Xml : ConfigurableJacksonXml(XmlMapper().let {
    it.registerModule(KotlinModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
        .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
    it
})