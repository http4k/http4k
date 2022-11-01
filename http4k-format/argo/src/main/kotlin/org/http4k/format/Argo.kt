package org.http4k.format

import argo.format.CompactJsonFormatter
import argo.format.PrettyJsonFormatter
import argo.jdom.JdomParser
import argo.jdom.JsonNode
import argo.jdom.JsonNodeFactories
import argo.jdom.JsonNodeFactories.field
import argo.jdom.JsonNodeFactories.`object`
import argo.jdom.JsonNodeType.ARRAY
import argo.jdom.JsonNodeType.FALSE
import argo.jdom.JsonNodeType.NULL
import argo.jdom.JsonNodeType.NUMBER
import argo.jdom.JsonNodeType.OBJECT
import argo.jdom.JsonNodeType.STRING
import argo.jdom.JsonNodeType.TRUE
import argo.jdom.JsonStringNode
import org.http4k.format.JsonType.Object
import java.math.BigDecimal
import java.math.BigInteger

object Argo : Json<JsonNode> {
    override fun typeOf(value: JsonNode): JsonType =
        when (value.type) {
            STRING -> JsonType.String
            TRUE -> JsonType.Boolean
            FALSE -> JsonType.Boolean
            NUMBER -> if (value.text.any { !it.isDigit() }) JsonType.Number else JsonType.Integer
            ARRAY -> JsonType.Array
            OBJECT -> Object
            NULL -> JsonType.Null
            else -> throw IllegalArgumentException("Don't know how to translate $value")
        }

    private val pretty = PrettyJsonFormatter()
    private val compact = CompactJsonFormatter()
    private val jdomParser = JdomParser()

    override fun String.asJsonObject(): JsonNode = let(jdomParser::parse)
    override fun String?.asJsonValue(): JsonNode = this?.let { JsonNodeFactories.string(it) }
        ?: JsonNodeFactories.nullNode()

    override fun Int?.asJsonValue(): JsonNode = this?.let { JsonNodeFactories.number(it.toLong()) }
        ?: JsonNodeFactories.nullNode()

    override fun Double?.asJsonValue(): JsonNode = this?.let { JsonNodeFactories.number(BigDecimal(it)) }
        ?: JsonNodeFactories.nullNode()

    override fun Long?.asJsonValue(): JsonNode = this?.let { JsonNodeFactories.number(it) }
        ?: JsonNodeFactories.nullNode()

    override fun BigDecimal?.asJsonValue(): JsonNode = this?.let { JsonNodeFactories.number(it) }
        ?: JsonNodeFactories.nullNode()

    override fun BigInteger?.asJsonValue(): JsonNode = this?.let { JsonNodeFactories.number(it) }
        ?: JsonNodeFactories.nullNode()

    override fun Boolean?.asJsonValue(): JsonNode = this?.let { JsonNodeFactories.booleanNode(it) }
        ?: JsonNodeFactories.nullNode()

    override fun <T : Iterable<JsonNode>> T.asJsonArray(): JsonNode = JsonNodeFactories.array(this)
    override fun JsonNode.asPrettyJsonString(): String = pretty.format(this)
    override fun JsonNode.asCompactJsonString(): String = compact.format(this)

    override fun <LIST : Iterable<Pair<String, JsonNode>>> LIST.asJsonObject(): JsonNode =
        `object`(associate { JsonNodeFactories.string(it.first) to it.second })

    override fun fields(node: JsonNode) =
        if (typeOf(node) != Object) emptyList() else node.fieldList.map { it.name.text to it.value }

    override fun elements(value: JsonNode): Iterable<JsonNode> = value.elements
    override fun text(value: JsonNode): String = value.text
    override fun bool(value: JsonNode): Boolean = value.getBooleanValue()
    override fun integer(value: JsonNode) = value.getNumberValue().toLong()
    override fun decimal(value: JsonNode) = value.getNumberValue().toBigDecimal()

    override fun textValueOf(node: JsonNode, name: String): String = with(node.getNode(name)) {
        when (type) {
            STRING -> text
            TRUE -> "true"
            FALSE -> "false"
            NUMBER -> text
            else -> throw IllegalArgumentException("Don't know how to translate $node")
        }
    }
}
