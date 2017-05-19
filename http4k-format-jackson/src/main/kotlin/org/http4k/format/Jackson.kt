package org.http4k.format

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BigIntegerNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DecimalNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.core.Body
import org.http4k.lens.BiDiBodyLens
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

open class ConfigurableJackson(private val mapper: ObjectMapper) : Json<JsonNode, JsonNode> {

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
    override fun Int?.asJsonValue(): JsonNode = this?.let { IntNode(this) } ?: NullNode.instance
    override fun Double?.asJsonValue(): JsonNode = this?.let { DecimalNode(BigDecimal(this)) } ?: NullNode.instance
    override fun Long?.asJsonValue(): JsonNode = this?.let { LongNode(this) } ?: NullNode.instance
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

    fun Any.asJsonNode(): JsonNode = mapper.convertValue(this, JsonNode::class.java)
    fun <T : Any> String.asA(c: KClass<T>): T = mapper.convertValue(this.asJsonObject(), c.java)
    fun <T : Any> JsonNode.asA(c: KClass<T>): T = mapper.convertValue(this, c.java)

    inline fun <reified T : Any> String.asA(): T = asA(T::class)
    inline fun <reified T : Any> JsonNode.asA(): T = asA(T::class)

    inline fun <reified T : Any> Body.Companion.auto(description : String? = null): BiDiBodyLens<T> = Body.json(description).map({ it.asA<T>() }, { it.asJsonNode() }).toLens()

    fun Any.asJsonString(): String = asJsonNode().asCompactJsonString()
}

object Jackson : ConfigurableJackson(ObjectMapper()
    .registerModule(KotlinModule())
    .configure(FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(USE_BIG_DECIMAL_FOR_FLOATS, true)
    .configure(USE_BIG_INTEGER_FOR_INTS, true)
)
