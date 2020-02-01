package org.http4k.format

import kotlinx.serialization.json.*
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.SerialModule
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.serialization.json.Json as KotlinxJson

/**
 * To implement custom JSON configuration, create your own object singleton. Extra mappings can be added before done() is called.
 */
open class ConfigurableKotlinxSerialization(
    config: JsonConfiguration = JsonConfiguration.Stable,
    context: SerialModule = EmptyModule
) : Json<JsonElement> {

    private val json = KotlinxJson(config, context)
    private val prettyJson = KotlinxJson(config.copy(prettyPrint = true, indent = "\t"), context)

    override fun typeOf(value: JsonElement): JsonType =
        when (value) {
            is JsonLiteral -> when {
                value.isString -> JsonType.String
                value.booleanOrNull != null -> JsonType.Boolean
                value.doubleOrNull != null -> JsonType.Number
                else -> throw RuntimeException()
            }
            is JsonArray -> JsonType.Array
            is JsonObject -> JsonType.Object
            is JsonNull -> JsonType.Null
            else -> throw IllegalArgumentException("Don't know how to translate $value")
        }

    override fun JsonElement.asPrettyJsonString() = prettyJson.stringify(JsonElement.serializer(), this)

    override fun JsonElement.asCompactJsonString() = json.stringify(JsonElement.serializer(), this)

    override fun String.asJsonObject() = json.parse(JsonObjectSerializer, this)

    override fun String?.asJsonValue() = JsonPrimitive(this)

    override fun Int?.asJsonValue() = JsonPrimitive(this)

    override fun Double?.asJsonValue() = JsonPrimitive(this)

    override fun Long?.asJsonValue() = JsonPrimitive(this)

    override fun BigDecimal?.asJsonValue() = JsonPrimitive("$this")

    override fun BigInteger?.asJsonValue() = JsonPrimitive(this)

    override fun Boolean?.asJsonValue() = JsonPrimitive(this)

    override fun <T : Iterable<JsonElement>> T.asJsonArray() = JsonArray(this.toList())

    override fun <LIST : Iterable<Pair<String, JsonElement>>> LIST.asJsonObject() = JsonObject(this.toMap())

    override fun fields(node: JsonElement) =
        if (node !is JsonObject) emptyList() else node.toList()

    override fun elements(value: JsonElement) = when (value) {
        is JsonObject -> value.values
        is JsonArray -> value.content
        else -> emptyList()
    }

    override fun text(value: JsonElement) =
        value.content

    override fun bool(value: JsonElement) = value.boolean

    override fun integer(value: JsonElement) = value.long

    override fun decimal(value: JsonElement): BigDecimal = BigDecimal(value.content)

    override fun textValueOf(node: JsonElement, name: String) = when (node) {
        is JsonObject -> node[name]?.content
        else -> throw IllegalArgumentException("node is not an object")
    }
}
