package org.http4k.format

import com.ubertob.kondor.json.JInstance
import com.ubertob.kondor.json.JStringRepresentable
import com.ubertob.kondor.json.JsonConverter
import com.ubertob.kondor.json.JsonOutcome
import com.ubertob.kondor.json.jsonnode.BooleanNode
import com.ubertob.kondor.json.jsonnode.JsonNode
import com.ubertob.kondor.json.jsonnode.JsonNodeArray
import com.ubertob.kondor.json.jsonnode.JsonNodeBoolean
import com.ubertob.kondor.json.jsonnode.JsonNodeNull
import com.ubertob.kondor.json.jsonnode.JsonNodeNumber
import com.ubertob.kondor.json.jsonnode.JsonNodeObject
import com.ubertob.kondor.json.jsonnode.JsonNodeString
import com.ubertob.kondor.json.jsonnode.NodeKind
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.NodePathRoot
import com.ubertob.kondor.json.jsonnode.NodePathSegment
import com.ubertob.kondor.json.jsonnode.NumberNode
import com.ubertob.kondor.json.jsonnode.parseJsonNode
import com.ubertob.kondor.json.parser.pretty
import com.ubertob.kondor.json.tryFromNode
import org.http4k.lens.BiDiMapping
import java.io.InputStream
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

open class ConfigurableKondorJson(init: Registry.() -> Registry = { this }) : AutoMarshallingJson<JsonNode>() {

    private val registry = Registry()

    init {
        init(registry)
    }

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
        is JsonNodeNumber -> value.num.longValueExact()
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

    // auto

    override fun <T : Any> asA(input: InputStream, target: KClass<T>): T = registry.converterFor(target).fromJson(input)
    override fun <T : Any> asA(input: String, target: KClass<T>): T = registry.converterFor(target).fromJson(input)
    override fun <T : Any> asA(j: JsonNode, target: KClass<T>): T = registry.converterFor(target).fromJsonNode(j)
    override fun asJsonObject(input: Any): JsonNode = registry.converterFor(input).toJsonNode(input)

    class Registry {
        private val converters = mutableListOf<ConverterWrapper<*, *>>()

        init {
            register(Unit::class.java, JInstance(Unit))
        }

        internal fun <T, JN : JsonNode> register(target: Class<T>, converter: JsonConverter<T, JN>) {
            converters += ConverterWrapper(target, converter)
        }

        @Suppress("UNCHECKED_CAST")
        internal fun <T : Any> converterFor(target: KClass<T>): ConverterWrapper<T, *> =
            requireNotNull(converters.find { it.canConvert(target) } as? ConverterWrapper<T, *>) {
                "JsonConverter for '$target' has not been registered"
            }

        internal fun converterFor(input: Any): ConverterWrapper<*, *> =
            requireNotNull(converters.find { it.canConvert(input) }) {
                "JsonConverter for '${input::class}' has not been registered"
            }
    }
}

internal class ConverterWrapper<T, JN: JsonNode>(private val target: Class<T>, private val converter: JsonConverter<T, JN>) {
    fun canConvert(input: Any) = target.isInstance(input)
    fun canConvert(clazz: KClass<*>) = target == clazz.java

    @Suppress("UNCHECKED_CAST")
    fun toJsonNode(input: Any): JN = converter.toJsonNode(input as T, NodePathRoot)
    fun fromJson(json: String): T = converter.fromJson(json).orThrow()
    fun fromJson(stream: InputStream): T = converter.fromJson(stream).orThrow()
    @Suppress("UNCHECKED_CAST")
    fun fromJsonNode(node: JsonNode): T = converter.fromJsonNode(node as JN).orThrow()
}

class KondorJsonAutoMappingConfiguration(private val registry : ConfigurableKondorJson.Registry) : AutoMappingConfiguration<ConfigurableKondorJson.Registry> {

    inline fun <reified T: Any, JN : JsonNode> register(converter: JsonConverter<T, JN>) = apply {
        register(T::class, converter)
    }

    fun <T: Any, JN : JsonNode> register(target: KClass<T>, converter: JsonConverter<T, JN>) = apply {
        registry.register(target.java, converter)
    }


    override fun <OUT> boolean(mapping: BiDiMapping<Boolean, OUT>) = apply {
        registry.register(mapping.clazz, object : JsonConverter<OUT, JsonNodeBoolean> {
            override val _nodeType: NodeKind<JsonNodeBoolean> = BooleanNode
            override fun fromJsonNode(node: JsonNodeBoolean): JsonOutcome<OUT> = tryFromNode(node) { mapping(node.value) }
            override fun toJsonNode(value: OUT, path: NodePath): JsonNodeBoolean = JsonNodeBoolean(mapping(value), path)
        })
    }

    override fun <OUT> int(mapping: BiDiMapping<Int, OUT>) = apply {
        registry.register(mapping.clazz, object : JsonConverter<OUT, JsonNodeNumber> {
            override val _nodeType: NodeKind<JsonNodeNumber> = NumberNode
            override fun fromJsonNode(node: JsonNodeNumber): JsonOutcome<OUT> = tryFromNode(node) { mapping(node.num.intValueExact()) }
            override fun toJsonNode(value: OUT, path: NodePath): JsonNodeNumber = JsonNodeNumber(mapping(value).toBigDecimal(), path)
        })
    }

    override fun <OUT> long(mapping: BiDiMapping<Long, OUT>) = apply {
        registry.register(mapping.clazz, object : JsonConverter<OUT, JsonNodeNumber> {
            override val _nodeType: NodeKind<JsonNodeNumber> = NumberNode
            override fun fromJsonNode(node: JsonNodeNumber): JsonOutcome<OUT> = tryFromNode(node) { mapping(node.num.longValueExact()) }
            override fun toJsonNode(value: OUT, path: NodePath): JsonNodeNumber = JsonNodeNumber(mapping(value).toBigDecimal(), path)
        })
    }

    override fun <OUT> double(mapping: BiDiMapping<Double, OUT>) = apply {
        registry.register(mapping.clazz, object : JsonConverter<OUT, JsonNodeNumber> {
            override val _nodeType: NodeKind<JsonNodeNumber> = NumberNode
            override fun fromJsonNode(node: JsonNodeNumber): JsonOutcome<OUT> = tryFromNode(node) { mapping(node.num.toDouble()) }
            override fun toJsonNode(value: OUT, path: NodePath): JsonNodeNumber = JsonNodeNumber(mapping(value).toBigDecimal(), path)
        })
    }

    override fun <OUT> bigInteger(mapping: BiDiMapping<BigInteger, OUT>) = apply {
        registry.register(mapping.clazz, object : JsonConverter<OUT, JsonNodeNumber> {
            override val _nodeType: NodeKind<JsonNodeNumber> = NumberNode
            override fun fromJsonNode(node: JsonNodeNumber): JsonOutcome<OUT> = tryFromNode(node) { mapping(node.num.toBigIntegerExact()) }
            override fun toJsonNode(value: OUT, path: NodePath): JsonNodeNumber = JsonNodeNumber(mapping(value).toBigDecimal(), path)
        })
    }

    override fun <OUT> bigDecimal(mapping: BiDiMapping<BigDecimal, OUT>) = apply {
        registry.register(mapping.clazz, object : JsonConverter<OUT, JsonNodeNumber> {
            override val _nodeType: NodeKind<JsonNodeNumber> = NumberNode
            override fun fromJsonNode(node: JsonNodeNumber): JsonOutcome<OUT> = tryFromNode(node) { mapping(node.num) }
            override fun toJsonNode(value: OUT, path: NodePath): JsonNodeNumber = JsonNodeNumber(mapping(value), path)
        })
    }

    override fun <OUT> text(mapping: BiDiMapping<String, OUT>) = apply {
        registry.register(mapping.clazz, object : JStringRepresentable<OUT>() {
            override val cons: (String) -> OUT = mapping::invoke
            override val render: (OUT) -> String = mapping::invoke
        })
    }

    override fun done(): ConfigurableKondorJson.Registry = registry
}

fun ConfigurableKondorJson.Registry.asConfigurable() = KondorJsonAutoMappingConfiguration(this)

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

