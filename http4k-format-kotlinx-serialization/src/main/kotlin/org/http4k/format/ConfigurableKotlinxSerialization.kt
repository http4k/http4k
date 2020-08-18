package org.http4k.format

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectSerializer
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.serialization.json.Json as KotlinxJson

open class ConfigurableKotlinxSerialization(
    json: JsonBuilder.() -> Unit,
) : Json<JsonElement> {
    private val json = KotlinxJson { json() }
    private val prettyJson =
        KotlinxJson {
            json()
            prettyPrint = true
        }

    override fun typeOf(value: JsonElement) = when (value) {
        is JsonNull -> JsonType.Null
        is JsonPrimitive -> when {
            value.isString -> JsonType.String
            value.booleanOrNull != null -> JsonType.Boolean
            value.doubleOrNull != null -> JsonType.Number
            else -> throw RuntimeException()
        }
        is JsonArray -> JsonType.Array
        is JsonObject -> JsonType.Object
        else -> throw IllegalArgumentException("Don't know how to translate $value")
    }

    override fun JsonElement.asPrettyJsonString() = prettyJson.encodeToString(JsonElement.serializer(), this)

    override fun JsonElement.asCompactJsonString() = json.encodeToString(JsonElement.serializer(), this)

    override fun String.asJsonObject() = json.decodeFromString(JsonObjectSerializer, this)

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
        is JsonArray -> value.jsonArray
        else -> emptyList()
    }

    override fun text(value: JsonElement) = value.jsonPrimitive.content

    override fun bool(value: JsonElement) = value.jsonPrimitive.boolean

    override fun integer(value: JsonElement) = value.jsonPrimitive.long

    override fun decimal(value: JsonElement): BigDecimal = BigDecimal(value.jsonPrimitive.content)

    override fun textValueOf(node: JsonElement, name: String) = when (node) {
        is JsonObject -> node[name]?.let { it.jsonPrimitive.content }
        else -> throw IllegalArgumentException("node is not an object")
    }
}
