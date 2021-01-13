package org.http4k.format

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BigIntegerNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DecimalNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.readValue
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.ContentNegotiation.Companion.None
import org.http4k.lens.string
import org.http4k.websocket.WsMessage
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

open class ConfigurableJackson(val mapper: ObjectMapper,
                               val defaultContentType: ContentType = APPLICATION_JSON) : AutoMarshallingJson<JsonNode>() {

    override fun typeOf(value: JsonNode): JsonType = when (value) {
        is TextNode -> JsonType.String
        is BooleanNode -> JsonType.Boolean
        is NumericNode -> JsonType.Number
        is ArrayNode -> JsonType.Array
        is ObjectNode -> JsonType.Object
        is NullNode -> JsonType.Null
        else -> throw IllegalArgumentException("Don't know now to translate $value")
    }

    override fun String.asJsonObject(): JsonNode = mapper.readValue(this, JsonNode::class.java)
    override fun String?.asJsonValue(): JsonNode = this?.let { TextNode(this) } ?: NullNode.instance
    override fun Int?.asJsonValue(): JsonNode = this?.let { BigIntegerNode(toBigInteger()) } ?: NullNode.instance
    override fun Double?.asJsonValue(): JsonNode = this?.let { DecimalNode(BigDecimal(this)) } ?: NullNode.instance
    override fun Long?.asJsonValue(): JsonNode = this?.let { BigIntegerNode(toBigInteger()) } ?: NullNode.instance
    override fun BigDecimal?.asJsonValue(): JsonNode = this?.let { DecimalNode(this) } ?: NullNode.instance
    override fun BigInteger?.asJsonValue(): JsonNode = this?.let { BigIntegerNode(this) } ?: NullNode.instance
    override fun Boolean?.asJsonValue(): JsonNode = this?.let { BooleanNode.valueOf(this) } ?: NullNode.instance
    override fun <T : Iterable<JsonNode>> T.asJsonArray(): JsonNode = mapper.createArrayNode().also { it.addAll(toList()) }
    override fun JsonNode.asPrettyJsonString(): String = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)
    override fun JsonNode.asCompactJsonString(): String = mapper.writeValueAsString(this)
    override fun <LIST : Iterable<Pair<String, JsonNode>>> LIST.asJsonObject(): JsonNode =
        mapper.createObjectNode().also { it.setAll<JsonNode>(mapOf(*toList().toTypedArray())) }

    override fun fields(node: JsonNode) = node.fields().asSequence().map { (key, value) -> key to value }.toList()

    override fun elements(value: JsonNode) = value.elements().asSequence().asIterable()
    override fun text(value: JsonNode): String = value.asText()
    override fun bool(value: JsonNode): Boolean = value.asBoolean()
    override fun integer(value: JsonNode) = value.asLong()
    override fun decimal(value: JsonNode) = BigDecimal(value.toString())
    override fun textValueOf(node: JsonNode, name: String) = node[name]?.asText()

    // auto
    override fun asJsonObject(input: Any): JsonNode = mapper.convertValue(input, JsonNode::class.java)

    override fun <T : Any> asA(input: String, target: KClass<T>): T = mapper.readValue(input, target.java)
    override fun <T : Any> asA(j: JsonNode, target: KClass<T>): T = mapper.convertValue(j, target.java)

    inline fun <reified T : Any> JsonNode.asA(): T = mapper.convertValue(this)

    inline fun <reified T : Any> WsMessage.Companion.auto() = WsMessage.string().map(mapper.read<T>(), mapper.write())

    inline fun <reified T : Any> Body.Companion.auto(description: String? = null,
                                                     contentNegotiation: ContentNegotiation = None,
                                                     contentType: ContentType = defaultContentType) = autoBody<T>(description, contentNegotiation, contentType)

    inline fun <reified T : Any> autoBody(description: String? = null,
                                          contentNegotiation: ContentNegotiation = None,
                                          contentType: ContentType = defaultContentType)
        : BiDiBodyLensSpec<T> = httpBodyLens(description, contentNegotiation, contentType).map(mapper.read(), mapper.write())

    // views
    fun <T : Any, V : Any> T.asCompactJsonStringUsingView(v: KClass<V>): String = mapper.writerWithView(v.java).writeValueAsString(this)

    fun <T : Any, V : Any> String.asUsingView(t: KClass<T>, v: KClass<V>): T = mapper.readerWithView(v.java).forType(t.java).readValue(this)

    inline fun <reified T : Any, reified V : Any> Body.Companion.autoView(description: String? = null,
                                                                          contentNegotiation: ContentNegotiation = None,
                                                                          contentType: ContentType = APPLICATION_JSON) =
        Body.string(contentType, description, contentNegotiation).map({ it.asUsingView(T::class, V::class) }, { it.asCompactJsonStringUsingView(V::class) })

    inline fun <reified T : Any, reified V : Any> WsMessage.Companion.autoView() =
        WsMessage.string().map({ it.asUsingView(T::class, V::class) }, { it.asCompactJsonStringUsingView(V::class) })
}

fun KotlinModule.asConfigurable() = asConfigurable(ObjectMapper())

inline fun <reified T : Any> ObjectMapper.read(): (String) -> T = { readValue(it) }

inline fun <reified T : Any> ObjectMapper.write(): (T) -> String = {
    with(this) {
        val typeRef = jacksonTypeRef<T>()
        when {
            typeFactory.constructType(typeRef).isContainerType -> writer().forType(typeRef).writeValueAsString(it)
            else -> writeValueAsString(it)
        }
    }
}
