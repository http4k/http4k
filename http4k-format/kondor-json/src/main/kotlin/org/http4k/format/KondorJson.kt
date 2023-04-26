package org.http4k.format

import com.ubertob.kondor.json.jsonnode.JsonNode
import com.ubertob.kondor.json.jsonnode.JsonNodeArray
import com.ubertob.kondor.json.jsonnode.JsonNodeBoolean
import com.ubertob.kondor.json.jsonnode.JsonNodeNull
import com.ubertob.kondor.json.jsonnode.JsonNodeNumber
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.JsonNodeString
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.json.jsonnode.NodePathSegment
import com.ubertob.kondor.json.jsonnode.parseJsonNode
import com.ubertob.kondor.json.parser.pretty
import java.math.BigDecimal
import java.math.BigInteger

object KondorJson : Json<JsonNode> {
    override fun typeOf(value: JsonNode): JsonType =
        when (value) {
            is JsonNodeArray -> JsonType.Array
            is JsonNodeBoolean -> JsonType.Boolean
            is JsonNodeNull -> JsonType.Null
            is JsonNodeNumber -> if (value.num.scale() == 0) JsonType.Integer else JsonType.Number
            is JsonNodeObject -> JsonType.Object
            is JsonNodeString -> JsonType.String
        }

    override fun JsonNode.asPrettyJsonString(): String = this.pretty()
    override fun JsonNode.asCompactJsonString(): String = this.render()

    override fun String.asJsonObject() = parseJsonNode(this).orThrow()
    override fun String?.asJsonValue() = this?.let { JsonNodeString(it, NodePathRoot) } ?: JsonNodeNull(NodePathRoot)
    override fun Int?.asJsonValue() = this?.let { JsonNodeNumber(it.toBigDecimal(), NodePathRoot) } ?: JsonNodeNull(NodePathRoot)
    override fun Double?.asJsonValue() = this?.let { JsonNodeNumber(it.toBigDecimal(), NodePathRoot) } ?: JsonNodeNull(NodePathRoot)
    override fun Long?.asJsonValue() = this?.let { JsonNodeNumber(it.toBigDecimal(), NodePathRoot) } ?: JsonNodeNull(NodePathRoot)
    override fun BigDecimal?.asJsonValue() = this?.let { JsonNodeNumber(it, NodePathRoot) } ?: JsonNodeNull(NodePathRoot)
    override fun BigInteger?.asJsonValue() = this?.let { JsonNodeNumber(it.toBigDecimal(), NodePathRoot) } ?: JsonNodeNull(NodePathRoot)
    override fun Boolean?.asJsonValue() = this?.let { JsonNodeBoolean(it, NodePathRoot) } ?: JsonNodeNull(NodePathRoot)
    override fun <T : Iterable<JsonNode>> T.asJsonArray() = JsonNodeArray(this, NodePathRoot).updateNodePath()
    override fun <LIST : Iterable<Pair<String, JsonNode>>> LIST.asJsonObject() =
        JsonNodeObject(this.toMap(), NodePathRoot).updateNodePath()

    override fun fields(node: JsonNode) = if (node !is JsonNodeObject) emptyList() else node._fieldMap.map { it.key to it.value }

    override fun elements(value: JsonNode): Iterable<JsonNode> = when (value) {
        is JsonNodeObject -> value._fieldMap.map { it.value }
        is JsonNodeArray -> value.values
        else -> emptyList()
    }

    override fun text(value: JsonNode): String = when (value) {
        is JsonNodeString -> value.text
        is JsonNodeNumber -> value.num.toString()
        is JsonNodeBoolean -> value.value.toString()
        is JsonNodeArray -> ""
        is JsonNodeNull -> "null"
        is JsonNodeObject -> ""
    }

    override fun bool(value: JsonNode): Boolean = when (value) {
        is JsonNodeBoolean -> value.value
        is JsonNodeString -> value.text.toBooleanStrict()
        else -> throw IllegalArgumentException("The node type '${value.nodeKind.desc}' is not a boolean")
    }

    override fun integer(value: JsonNode): Long = when (value) {
        is JsonNodeNumber -> value.num.toLong()
        else -> throw IllegalArgumentException("The node type '${value.nodeKind.desc}' is not a number")
    }

    override fun decimal(value: JsonNode): BigDecimal = when (value) {
        is JsonNodeNumber -> value.num
        else -> throw IllegalArgumentException("The node type '${value.nodeKind.desc}' is not a number")
    }

    override fun textValueOf(node: JsonNode, name: String): String? =
        fields(node).firstOrNull { it.first == name }?.let {
            when (val foundNode = it.second) {
                is JsonNodeString -> foundNode.text
                is JsonNodeNumber -> foundNode.num.toString()
                is JsonNodeBoolean -> foundNode.value.toString()
                else -> null
            }
        }
}

// Lifted the render logic from kondor-json, but changed to not output blank spaces between fields and values,
// so that it is http4k json compliant.
private fun JsonNode.render(): String =
    when (this) {
        is JsonNodeNull -> "null"
        is JsonNodeString -> text.putInQuotes()
        is JsonNodeBoolean -> value.toString()
        is JsonNodeNumber -> num.toString()
        is JsonNodeArray -> values.joinToString(separator = ",", prefix = "[", postfix = "]") {
            it.render()
        }
        is JsonNodeObject -> _fieldMap.entries.joinToString(separator = ",", prefix = "{", postfix = "}") {
            it.key.putInQuotes() + ":" + it.value.render()
        }
    }

private val regex = """[\\"\n\r\t]""".toRegex()
private fun String.putInQuotes(): String =
    replace(regex) { m ->
        when (m.value) {
            "\\" -> "\\\\"
            "\"" -> "\\\""
            "\n" -> "\\n"
            "\b" -> "\\b"
            "\r" -> "\\r"
            "\t" -> "\\t"
            else -> ""
        }
    }.let { "\"${it}\"" }

private fun JsonNodeObject.updateNodePath(parentPath: NodePath = NodePathRoot): JsonNodeObject {
    val updatedFields = _fieldMap.map { (name, field) ->
        val nodePath = NodePathSegment(name, parentPath)
        name to when (field) {
            is JsonNodeObject -> field.updateNodePath(nodePath)
            is JsonNodeArray -> field.updateNodePath(nodePath)
            is JsonNodeNull -> field.copy(_path = nodePath)
            is JsonNodeString -> field.copy(_path = nodePath)
            is JsonNodeNumber -> field.copy(_path = nodePath)
            is JsonNodeBoolean -> field.copy(_path = nodePath)
        }
    }
    return this.copy(_fieldMap = updatedFields.toMap(), _path = parentPath)
}

private fun JsonNodeArray.updateNodePath(parentPath: NodePath = NodePathRoot): JsonNodeArray {
    val updatedValues = values.map { item ->
        when (item) {
            is JsonNodeObject -> item.updateNodePath(parentPath)
            is JsonNodeArray -> item.updateNodePath(parentPath)
            is JsonNodeNull -> item.copy(_path = parentPath)
            is JsonNodeString -> item.copy(_path = parentPath)
            is JsonNodeNumber -> item.copy(_path = parentPath)
            is JsonNodeBoolean -> item.copy(_path = parentPath)
        }
    }
    return this.copy(values = updatedValues, _path = parentPath)
}
