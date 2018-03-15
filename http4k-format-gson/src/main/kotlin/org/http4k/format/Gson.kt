package org.http4k.format

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import org.http4k.core.Body
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.BiDiWsMessageLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.websocket.WsMessage
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

class InvalidJsonException(messasge: String, cause: Throwable? = null) : Exception(messasge, cause)

open class ConfigurableGson(builder: GsonBuilder) : JsonLibAutoMarshallingJson<JsonElement>() {
    override fun typeOf(value: JsonElement): JsonType =
        when {
            value.isJsonArray -> JsonType.Array
            value.isJsonNull -> JsonType.Null
            value.isJsonObject -> JsonType.Object
            value.isJsonPrimitive -> {
                val prim = value.asJsonPrimitive
                when {
                    prim.isBoolean -> JsonType.Boolean
                    prim.isNumber -> JsonType.Number
                    prim.isString -> JsonType.String
                    else -> throw IllegalArgumentException("Don't know now to translate $value")
                }
            }
            else -> throw IllegalArgumentException("Don't know now to translate $value")
        }

    private val compact = builder.create()
    private val pretty = builder.setPrettyPrinting().create()

    override fun String.asJsonObject(): JsonElement = JsonParser().parse(this).let { if(it.isJsonArray || it.isJsonObject) it else throw InvalidJsonException(
        "Could not convert to a JSON Object or Array. $this"
    ) }
    override fun String?.asJsonValue(): JsonElement = this?.let { JsonPrimitive(this) } ?: JsonNull.INSTANCE
    override fun Int?.asJsonValue(): JsonElement = this?.let { JsonPrimitive(this) } ?: JsonNull.INSTANCE
    override fun Double?.asJsonValue(): JsonElement = this?.let { JsonPrimitive(this) } ?: JsonNull.INSTANCE
    override fun Long?.asJsonValue(): JsonElement = this?.let { JsonPrimitive(this) } ?: JsonNull.INSTANCE
    override fun BigDecimal?.asJsonValue(): JsonElement = this?.let { JsonPrimitive(this) } ?: JsonNull.INSTANCE
    override fun BigInteger?.asJsonValue(): JsonElement = this?.let { JsonPrimitive(this) } ?: JsonNull.INSTANCE
    override fun Boolean?.asJsonValue(): JsonElement = this?.let { JsonPrimitive(this) } ?: JsonNull.INSTANCE
    override fun <T : Iterable<JsonElement>> T.asJsonArray(): JsonElement = fold(JsonArray()) { memo, o -> memo.add(o); memo }

    override fun JsonElement.asPrettyJsonString(): String = pretty.toJson(this)
    override fun JsonElement.asCompactJsonString(): String = compact.toJson(this)
    override fun <LIST : Iterable<Pair<String, JsonElement>>> LIST.asJsonObject(): JsonElement {
        val root = JsonObject()
        forEach { root.add(it.first, it.second) }
        return root
    }

    override fun fields(node: JsonElement): Iterable<Pair<String, JsonElement>> {
        val fieldList = mutableListOf<Pair<String, JsonElement>>()
        for ((key, value) in node.asJsonObject.entrySet()) {
            fieldList += key to value
        }
        return fieldList
    }

    override fun elements(value: JsonElement): Iterable<JsonElement> = value.asJsonArray
    override fun text(value: JsonElement): String = value.asString

    override fun asJsonObject(a: Any): JsonElement = compact.toJsonTree(a)
    override fun <T : Any> asA(s: String, c: KClass<T>): T = compact.fromJson(s, c.java)
    override fun <T : Any> asA(j: JsonElement, c: KClass<T>): T = compact.fromJson(j, c.java)

    inline fun <reified T : Any> String.asA(): T = asA(this, T::class)
    inline fun <reified T : Any> JsonElement.asA(): T = asA(this, T::class)

    inline fun <reified T : Any> Body.Companion.auto(description: String? = null, contentNegotiation: ContentNegotiation = ContentNegotiation.None): BiDiBodyLensSpec<T> = Body.json(description, contentNegotiation).map({ it.asA<T>() }, { it.asJsonObject() })

    inline fun <reified T : Any> WsMessage.Companion.auto(): BiDiWsMessageLensSpec<T> = WsMessage.json().map({ it.asA<T>() }, { it.asJsonObject() })
}

object Gson : ConfigurableGson(GsonBuilder().serializeNulls())
