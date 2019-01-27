package org.http4k.format

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BigIntegerNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DecimalNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.core.Body
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.BiDiMapping
import org.http4k.lens.BiDiWsMessageLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.duration
import org.http4k.lens.instant
import org.http4k.lens.localDate
import org.http4k.lens.localDateTime
import org.http4k.lens.localTime
import org.http4k.lens.offsetDateTime
import org.http4k.lens.offsetTime
import org.http4k.lens.regexObject
import org.http4k.lens.uri
import org.http4k.lens.url
import org.http4k.lens.uuid
import org.http4k.lens.zonedDateTime
import org.http4k.websocket.WsMessage
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

open class ConfigurableJackson(private val mapper: ObjectMapper) : JsonLibAutoMarshallingJson<JsonNode>() {

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
    override fun Int?.asJsonValue(): JsonNode = this?.let { BigIntegerNode(this.toBigInteger()) } ?: NullNode.instance
    override fun Double?.asJsonValue(): JsonNode = this?.let { DecimalNode(BigDecimal(this)) } ?: NullNode.instance
    override fun Long?.asJsonValue(): JsonNode = this?.let { BigIntegerNode(this.toBigInteger()) } ?: NullNode.instance
    override fun BigDecimal?.asJsonValue(): JsonNode = this?.let { DecimalNode(this) } ?: NullNode.instance
    override fun BigInteger?.asJsonValue(): JsonNode = this?.let { BigIntegerNode(this) } ?: NullNode.instance
    override fun Boolean?.asJsonValue(): JsonNode = this?.let { BooleanNode.valueOf(this) } ?: NullNode.instance
    override fun <T : Iterable<JsonNode>> T.asJsonArray(): JsonNode {
        val root = mapper.createArrayNode()
        root.addAll(this.toList())
        return root
    }

    override fun JsonNode.asPrettyJsonString(): String = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)
    override fun JsonNode.asCompactJsonString(): String = mapper.writeValueAsString(this)
    override fun <LIST : Iterable<Pair<String, JsonNode>>> LIST.asJsonObject(): JsonNode {
        val root = mapper.createObjectNode()
        root.setAll(mapOf(*this.toList().toTypedArray()))
        return root
    }

    override fun fields(node: JsonNode): Iterable<Pair<String, JsonNode>> {
        val fieldList = mutableListOf<Pair<String, JsonNode>>()
        for ((key, value) in node.fields()) {
            fieldList += key to value
        }
        return fieldList
    }

    override fun elements(value: JsonNode): Iterable<JsonNode> = value.elements().asSequence().asIterable()
    override fun text(value: JsonNode): String = value.asText()
    override fun bool(value: JsonNode): Boolean = value.asBoolean()

    override fun asJsonObject(a: Any): JsonNode = mapper.convertValue(a, JsonNode::class.java)
    override fun <T : Any> asA(s: String, c: KClass<T>): T = mapper.convertValue(s.asJsonObject(), c.java)
    override fun <T : Any> asA(j: JsonNode, c: KClass<T>): T = mapper.convertValue(j, c.java)

    inline fun <reified T : Any> JsonNode.asA(): T = asA(this, T::class)

    inline fun <reified T : Any> Body.Companion.auto(description: String? = null, contentNegotiation: ContentNegotiation = ContentNegotiation.None): BiDiBodyLensSpec<T> = Body.json(description, contentNegotiation).map({ it.asA<T>() }, { it.asJsonObject() })

    inline fun <reified T : Any> WsMessage.Companion.auto(): BiDiWsMessageLensSpec<T> = WsMessage.json().map({ it.asA<T>() }, { it.asJsonObject() })

    override fun textValueOf(node: JsonNode, name: String) = node[name]?.asText()
}

val defaultKotlinModuleWithHttp4kSerialisers = KotlinModule()
    .custom(BiDiMapping.duration())
    .custom(BiDiMapping.uri())
    .custom(BiDiMapping.url())
    .custom(BiDiMapping.uuid())
    .custom(BiDiMapping.regexObject())
    .custom(BiDiMapping.instant())
    .custom(BiDiMapping.localTime())
    .custom(BiDiMapping.localDate())
    .custom(BiDiMapping.localDateTime())
    .custom(BiDiMapping.zonedDateTime())
    .custom(BiDiMapping.offsetTime())
    .custom(BiDiMapping.offsetDateTime())

object Jackson : ConfigurableJackson(ObjectMapper()
    .registerModule(defaultKotlinModuleWithHttp4kSerialisers)
    .disableDefaultTyping()
    .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(USE_BIG_DECIMAL_FOR_FLOATS, true)
    .configure(USE_BIG_INTEGER_FOR_INTS, true)
)

private inline fun <reified T> KotlinModule.custom(mapping: BiDiMapping<T>) =
    apply {
        addDeserializer(T::class.java, object : JsonDeserializer<T>() {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext): T = mapping.read(p.text)
        })
        addSerializer(T::class.java, object : JsonSerializer<T>() {
            override fun serialize(value: T?, gen: JsonGenerator, serializers: SerializerProvider) = gen.writeString(mapping.write(value!!))
        })
    }
