package org.http4k.format

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.http4k.lens.BiDiMapping
import java.lang.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

open class ConfigurableGson(builder: GsonBuilder) : JsonLibAutoMarshallingJson<JsonElement>() {

    override fun typeOf(value: JsonElement): JsonType =
        when {
            value.isJsonArray -> JsonType.Array
            value.isJsonNull -> JsonType.Null
            value.isJsonObject -> JsonType.Object
            value.isJsonPrimitive -> with(value.asJsonPrimitive) {
                when {
                    isBoolean -> JsonType.Boolean
                    isNumber -> JsonType.Number
                    isString -> JsonType.String
                    else -> throw IllegalArgumentException("Don't know now to translate $value")
                }
            }
            else -> throw IllegalArgumentException("Don't know now to translate $value")
        }

    private val compact = builder.create()
    private val pretty = builder.setPrettyPrinting().create()

    override fun String.asJsonObject(): JsonElement = JsonParser().parse(this).let {
        if (it.isJsonArray || it.isJsonObject) it else throw InvalidJsonException(
            "Could not convert to a JSON Object or Array. $this"
        )
    }

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

    override fun fields(node: JsonElement): Iterable<Pair<String, JsonElement>> =
        if (typeOf(node) != JsonType.Object) emptyList() else {
            val fieldList = mutableListOf<Pair<String, JsonElement>>()
            for ((key, value) in node.asJsonObject.entrySet()) {
                fieldList += key to value
            }
            fieldList
        }

    override fun elements(value: JsonElement): Iterable<JsonElement> = value.asJsonArray
    override fun text(value: JsonElement): String = value.asString
    override fun bool(value: JsonElement): Boolean = value.asBoolean
    override fun integer(value: JsonElement) = value.asLong
    override fun decimal(value: JsonElement) = value.asBigDecimal

    override fun asJsonObject(input: Any): JsonElement = compact.toJsonTree(input)
    override fun <T : Any> asA(input: String, target: KClass<T>): T = compact.fromJson(input, target.java)
    override fun <T : Any> asA(j: JsonElement, target: KClass<T>): T = compact.fromJson(j, target.java)

    override fun textValueOf(node: JsonElement, name: String) = when (node) {
        is JsonObject -> node[name].asString
        else -> throw IllegalArgumentException("node is not an object")
    }
}

class InvalidJsonException(messasge: String, cause: Throwable? = null) : Exception(messasge, cause)

fun GsonBuilder.asConfigurable() = object : AutoMappingConfiguration<GsonBuilder> {
    override fun <OUT> int(mapping: BiDiMapping<Int, OUT>) = adapter(mapping, ::JsonPrimitive, JsonElement::getAsInt)
    override fun <OUT> long(mapping: BiDiMapping<Long, OUT>) = adapter(mapping, ::JsonPrimitive, JsonElement::getAsLong)
    override fun <OUT> double(mapping: BiDiMapping<Double, OUT>) = adapter(mapping, ::JsonPrimitive, JsonElement::getAsDouble)
    override fun <OUT> boolean(mapping: BiDiMapping<Boolean, OUT>) = adapter(mapping, ::JsonPrimitive, JsonElement::getAsBoolean)
    override fun <OUT> bigInteger(mapping: BiDiMapping<BigInteger, OUT>) = adapter(mapping, ::JsonPrimitive, JsonElement::getAsBigInteger)
    override fun <OUT> bigDecimal(mapping: BiDiMapping<BigDecimal, OUT>) = adapter(mapping, ::JsonPrimitive, JsonElement::getAsBigDecimal)
    override fun <OUT> text(mapping: BiDiMapping<String, OUT>) = adapter(mapping, ::JsonPrimitive, JsonElement::getAsString)

    private fun <IN, OUT> adapter(mapping: BiDiMapping<IN, OUT>, asPrimitive: IN.() -> JsonPrimitive, value: JsonElement.() -> IN) =
        apply {
            this@asConfigurable.registerTypeAdapter(mapping.clazz, object : JsonSerializer<OUT>, JsonDeserializer<OUT> {
                override fun serialize(src: OUT, typeOfSrc: Type, context: JsonSerializationContext) = mapping(src).asPrimitive()

                override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext) = mapping.invoke(json.value())
            })
        }

    override fun done(): GsonBuilder = this@asConfigurable
}