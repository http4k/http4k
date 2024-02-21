package org.http4k.format

import com.ubertob.kondor.json.JInstance
import com.ubertob.kondor.json.JsonConverter
import com.ubertob.kondor.json.JsonStyle
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
import com.ubertob.kondor.json.render
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpMessage
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.string
import org.http4k.websocket.WsMessage
import java.io.InputStream
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

class KondorJson(
    override val defaultContentType: ContentType = APPLICATION_JSON,
    private val compactJsonStyle: JsonStyle = JsonStyle.compactWithNulls,
    private val prettyJsonStyle: JsonStyle = JsonStyle.prettyWithNulls,
    init: InitContext.() -> Unit = {}
) :
    AutoMarshallingJson<JsonNode>() {

    inner class InitContext {
        fun <T : Any, JN : JsonNode> register(target: KClass<T>, converter: JsonConverter<T, JN>): InitContext = apply {
            register(target.java, converter)
        }
    }

    private val converters = mutableMapOf<Class<*>, JsonConverter<*, *>>()

    init {
        register(Unit::class.java, JInstance(Unit))
        init(InitContext())
    }

    override fun typeOf(value: JsonNode): JsonType =
        when (value) {
            is JsonNodeArray -> JsonType.Array
            is JsonNodeBoolean -> JsonType.Boolean
            is JsonNodeNull -> JsonType.Null
            is JsonNodeNumber -> if (value.num.toBigDecimal().scale() == 0) JsonType.Integer else JsonType.Number
            is JsonNodeObject -> JsonType.Object
            is JsonNodeString -> JsonType.String
        }

    override fun JsonNode.asPrettyJsonString(): String = this.render(prettyJsonStyle)
    override fun JsonNode.asCompactJsonString(): String = this.render(compactJsonStyle)

    override fun String.asJsonObject() = when (this) {
        "" -> error("Empty string found") // this is a bug in the kondor parser
        else -> parseJsonNode(this).orThrow()
    }

    override fun String?.asJsonValue() = this?.let { JsonNodeString(it, NodePathRoot) } ?: JsonNodeNull(NodePathRoot)
    override fun Int?.asJsonValue() =
        this?.let { JsonNodeNumber(it.toBigDecimal(), NodePathRoot) } ?: JsonNodeNull(NodePathRoot)

    override fun Double?.asJsonValue() =
        this?.let { JsonNodeNumber(it.toBigDecimal(), NodePathRoot) } ?: JsonNodeNull(NodePathRoot)

    override fun Long?.asJsonValue() =
        this?.let { JsonNodeNumber(it.toBigDecimal(), NodePathRoot) } ?: JsonNodeNull(NodePathRoot)

    override fun BigDecimal?.asJsonValue() =
        this?.let { JsonNodeNumber(it, NodePathRoot) } ?: JsonNodeNull(NodePathRoot)

    override fun BigInteger?.asJsonValue() =
        this?.let { JsonNodeNumber(it.toBigDecimal(), NodePathRoot) } ?: JsonNodeNull(NodePathRoot)

    override fun Boolean?.asJsonValue() = this?.let { JsonNodeBoolean(it, NodePathRoot) } ?: JsonNodeNull(NodePathRoot)
    override fun <T : Iterable<JsonNode>> T.asJsonArray() = JsonNodeArray(this, NodePathRoot).updateNodePath()
    override fun <LIST : Iterable<Pair<String, JsonNode>>> LIST.asJsonObject() =
        JsonNodeObject(this.toMap(), NodePathRoot).updateNodePath()

    override fun fields(node: JsonNode) = if (node !is JsonNodeObject) emptyList() else node._fieldMap.toList()

    override fun elements(value: JsonNode): Iterable<JsonNode> = when (value) {
        is JsonNodeArray -> value.elements
        else -> emptyList()
    }

    override fun text(value: JsonNode): String = when (value) {
        is JsonNodeString -> value.text
        is JsonNodeNumber -> value.num.toString()
        is JsonNodeBoolean -> value.boolean.toString()
        is JsonNodeArray -> ""
        is JsonNodeNull -> "null"
        is JsonNodeObject -> ""
    }

    override fun bool(value: JsonNode): Boolean = when (value) {
        is JsonNodeBoolean -> value.boolean
        is JsonNodeString -> value.text.toBooleanStrict()
        else -> throw IllegalArgumentException("The node type '${value.nodeKind.desc}' is not a boolean")
    }

    override fun integer(value: JsonNode): Long = when (value) {
        is JsonNodeNumber -> value.num.toBigDecimal().longValueExact()
        else -> throw IllegalArgumentException("The node type '${value.nodeKind.desc}' is not a number")
    }

    override fun decimal(value: JsonNode): BigDecimal = when (value) {
        is JsonNodeNumber -> value.num.toBigDecimal()
        else -> throw IllegalArgumentException("The node type '${value.nodeKind.desc}' is not a number")
    }

    override fun textValueOf(node: JsonNode, name: String): String? =
        fields(node).firstOrNull { it.first == name }?.let {
            when (val foundNode = it.second) {
                is JsonNodeString -> foundNode.text
                is JsonNodeNumber -> foundNode.num.toString()
                is JsonNodeBoolean -> foundNode.boolean.toString()
                else -> null
            }
        }

    // auto

    override fun <T : Any> asA(input: InputStream, target: KClass<T>): T = input.fromJson(converterFor(target))
    override fun <T : Any> asA(input: String, target: KClass<T>): T = input.fromJson(converterFor(target))
    override fun <T : Any> asA(j: JsonNode, target: KClass<T>): T = j.fromJsonNode(converterFor(target))
    override fun asJsonObject(input: Any): JsonNode = input.toJsonNode(converterFor(input))

    fun <T : Any> autoBody(
        target: KClass<T>,
        description: String? = null,
        contentNegotiation: ContentNegotiation = ContentNegotiation.None,
        contentType: ContentType = defaultContentType
    ) =
        converterFor(target).autoBody(description, contentNegotiation, contentType, compactJsonStyle)

    inline fun <reified T : Any> Body.Companion.auto(
        description: String? = null,
        contentNegotiation: ContentNegotiation = ContentNegotiation.None,
        contentType: ContentType = defaultContentType
    ) = autoBody<T>(description, contentNegotiation, contentType)

    inline fun <reified T : Any> autoBody(
        description: String? = null,
        contentNegotiation: ContentNegotiation = ContentNegotiation.None,
        contentType: ContentType = defaultContentType
    ) =
        autoBody(T::class, description, contentNegotiation, contentType)

    fun <T : Any> wsAutoBody(target: KClass<T>) = converterFor(target).wsAutoBody(compactJsonStyle)

    inline fun <reified T : Any> WsMessage.Companion.auto() = wsAutoBody(T::class)

// converter helpers

    private fun <T, JN : JsonNode> register(target: Class<T>, converter: JsonConverter<T, JN>) {
        converters[target] = converter
    }

    private fun <T, JN : JsonNode> InputStream.fromJson(converter: JsonConverter<T, JN>): T =
        converter.fromJson(this).orThrow()

    @Suppress("UNCHECKED_CAST")
    private fun <T, JN : JsonNode> JsonNode.fromJsonNode(converter: JsonConverter<T, JN>): T =
        converter.fromJsonNode(this as JN).orThrow()

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> converterFor(target: KClass<T>): JsonConverter<T, *> =
        requireNotNull(converters[target.java]) {
            "JsonConverter for '$target' has not been registered"
        } as JsonConverter<T, *>

    fun converterFor(input: Any): JsonConverter<*, *> =
        requireNotNull(converters.entries.find { it.key.isInstance(input) }) {
            "JsonConverter for '${input::class}' has not been registered"
        }.value
}

inline fun <reified T : Any, JN : JsonNode> KondorJson.InitContext.register(converter: JsonConverter<T, JN>) =
    register(T::class, converter)

fun <T : Any> JsonConverter<T, *>.autoBody(
    description: String? = null,
    contentNegotiation: ContentNegotiation = ContentNegotiation.None,
    contentType: ContentType = APPLICATION_JSON,
    renderStyle: JsonStyle = JsonStyle.compactWithNulls
) =
    httpBodyLens(description, contentNegotiation, contentType).map(
        { it.fromJson(this) },
        { it.toJsonNode(this).render(renderStyle) }
    )

fun <T : Any> JsonConverter<T, *>.wsAutoBody(renderStyle: JsonStyle = JsonStyle.compactWithNulls) =
    WsMessage.string().map(
        { it.fromJson(this) },
        { it.toJsonNode(this).render(renderStyle) }
    )

private fun <T, JN : JsonNode> String.fromJson(converter: JsonConverter<T, JN>): T =
    converter.fromJson(this).orThrow()

@Suppress("UNCHECKED_CAST")
private fun <T, JN : JsonNode> Any.toJsonNode(converter: JsonConverter<T, JN>) =
    converter.toJsonNode(this as T, NodePathRoot)

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
    val updatedValues = elements.map { item ->
        when (item) {
            is JsonNodeObject -> item.updateNodePath(parentPath)
            is JsonNodeArray -> item.updateNodePath(parentPath)
            is JsonNodeNull -> item.copy(_path = parentPath)
            is JsonNodeString -> item.copy(_path = parentPath)
            is JsonNodeNumber -> item.copy(_path = parentPath)
            is JsonNodeBoolean -> item.copy(_path = parentPath)
        }
    }
    return this.copy(elements = updatedValues, _path = parentPath)
}

inline operator fun <reified T : Any> KondorJson.invoke(msg: HttpMessage): T = autoBody<T>().toLens()(msg)
inline operator fun <reified T : Any, R : HttpMessage> KondorJson.invoke(item: T) = autoBody<T>().toLens().of<R>(item)
