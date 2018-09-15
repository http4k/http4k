package org.http4k.format

import argo.format.CompactJsonFormatter
import argo.format.PrettyJsonFormatter
import argo.jdom.JdomParser
import argo.jdom.JsonNode
import argo.jdom.JsonNodeFactories
import argo.jdom.JsonNodeFactories.`object`
import argo.jdom.JsonNodeType
import argo.jdom.JsonRootNode
import java.math.BigDecimal
import java.math.BigInteger

object Argo : Json<JsonRootNode, JsonNode> {
    override fun typeOf(value: JsonNode): JsonType =
        when (value.type) {
            JsonNodeType.STRING -> JsonType.String
            JsonNodeType.TRUE -> JsonType.Boolean
            JsonNodeType.FALSE -> JsonType.Boolean
            JsonNodeType.NUMBER -> JsonType.Number
            JsonNodeType.ARRAY -> JsonType.Array
            JsonNodeType.OBJECT -> JsonType.Object
            JsonNodeType.NULL -> JsonType.Null
            else -> throw IllegalArgumentException("Don't know now to translate $value")
        }

    private val pretty = PrettyJsonFormatter()
    private val compact = CompactJsonFormatter()
    private val jdomParser = JdomParser()

    override fun String.asJsonObject(): JsonRootNode = this.let(jdomParser::parse)
    override fun String?.asJsonValue(): JsonNode = this?.let { JsonNodeFactories.string(it) } ?: JsonNodeFactories.nullNode()
    override fun Int?.asJsonValue(): JsonNode = this?.let { JsonNodeFactories.number(it.toLong()) } ?: JsonNodeFactories.nullNode()
    override fun Double?.asJsonValue(): JsonNode = this?.let { JsonNodeFactories.number(BigDecimal(it)) } ?: JsonNodeFactories.nullNode()
    override fun Long?.asJsonValue(): JsonNode = this?.let { JsonNodeFactories.number(it) } ?: JsonNodeFactories.nullNode()
    override fun BigDecimal?.asJsonValue(): JsonNode = this?.let { JsonNodeFactories.number(it) } ?: JsonNodeFactories.nullNode()
    override fun BigInteger?.asJsonValue(): JsonNode = this?.let { JsonNodeFactories.number(it) } ?: JsonNodeFactories.nullNode()
    override fun Boolean?.asJsonValue(): JsonNode = this?.let { JsonNodeFactories.booleanNode(it) } ?: JsonNodeFactories.nullNode()
    override fun <T : Iterable<JsonNode>> T.asJsonArray(): JsonRootNode = JsonNodeFactories.array(this)
    override fun JsonRootNode.asPrettyJsonString(): String = pretty.format(this)
    override fun JsonRootNode.asCompactJsonString(): String = compact.format(this)
    override fun <LIST : Iterable<Pair<String, JsonNode>>> LIST.asJsonObject(): JsonRootNode = `object`(this.map { field(it.first, it.second) })
    override fun fields(node: JsonNode): Iterable<Pair<String, JsonNode>> = node.fieldList.map { it.name.text to it.value }
    override fun elements(value: JsonNode): Iterable<JsonNode> = value.elements
    override fun text(value: JsonNode): String = value.text
    override fun bool(value: JsonNode): Boolean = value.getBooleanValue()

    override fun textValueOf(node: JsonNode, name: String) = with(node.getNode(name)) {
         when (type) {
            JsonNodeType.STRING -> text
            JsonNodeType.TRUE -> "true"
            JsonNodeType.FALSE -> "false"
            JsonNodeType.NUMBER -> text
            else -> throw IllegalArgumentException("Don't know now to translate $node")
        }
    }

    private fun field(name: String, value: JsonNode) = JsonNodeFactories.field(name, value)
}
