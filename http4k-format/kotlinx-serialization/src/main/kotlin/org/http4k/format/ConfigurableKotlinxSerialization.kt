package org.http4k.format

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveKind.BOOLEAN
import kotlinx.serialization.descriptors.PrimitiveKind.DOUBLE
import kotlinx.serialization.descriptors.PrimitiveKind.INT
import kotlinx.serialization.descriptors.PrimitiveKind.LONG
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.serializer
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.lens.BiDiMapping
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.ContentNegotiation.Companion.None
import org.http4k.websocket.WsMessage
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass
import kotlinx.serialization.json.Json as KotlinxJson

open class ConfigurableKotlinxSerialization(
    json: JsonBuilder.() -> Unit,
    val defaultContentType: ContentType = APPLICATION_JSON
) : AutoMarshallingJson<JsonElement>() {
    val json = KotlinxJson { json() }
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

    override fun String.asJsonObject() = json.decodeFromString(JsonObject.serializer(), this)

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
        is JsonObject -> node[name]?.jsonPrimitive?.content
        else -> throw IllegalArgumentException("node is not an object")
    }

    // auto
    @ExperimentalSerializationApi
    override fun asJsonObject(input: Any): JsonElement =
        json.encodeToJsonElement(json.serializersModule.serializer(input::class.java), input)

    @ExperimentalSerializationApi
    override fun <T : Any> asA(j: JsonElement, target: KClass<T>): T {
        @Suppress("UNCHECKED_CAST")
        return json.decodeFromJsonElement(json.serializersModule.serializer(target.java), j) as T
    }

    override fun <T : Any> asA(input: String, target: KClass<T>): T =
        json.parseToJsonElement(input).asA(target)

    inline fun <reified T : Any> JsonElement.asA(): T = json.decodeFromJsonElement(this)

    inline fun <reified T : Any> WsMessage.Companion.auto() = WsMessage.json().map({ it.asA<T>() }, { it.asJsonObject() })

    inline fun <reified T : Any> Body.Companion.auto(description: String? = null,
                                                     contentNegotiation: ContentNegotiation = None,
                                                     contentType: ContentType = defaultContentType) = autoBody<T>(description, contentNegotiation, contentType)

    inline fun <reified T : Any> autoBody(description: String? = null,
                                          contentNegotiation: ContentNegotiation = None,
                                          contentType: ContentType = defaultContentType) =
        httpBodyLens(description, contentNegotiation, contentType).map({ json.decodeFromString<T>(it) }, { json.encodeToString(it) })
}

fun JsonBuilder.asConfigurable() = object : AutoMappingConfiguration<JsonBuilder> {

    override fun <OUT> boolean(mapping: BiDiMapping<Boolean, OUT>) = adapter(mapping, Decoder::decodeBoolean, Encoder::encodeBoolean, "BooleanSerializer", BOOLEAN)
    override fun <OUT> int(mapping: BiDiMapping<Int, OUT>) = adapter(mapping, Decoder::decodeInt, Encoder::encodeInt, "IntSerializer", INT)
    override fun <OUT> long(mapping: BiDiMapping<Long, OUT>) = adapter(mapping, Decoder::decodeLong, Encoder::encodeLong, "LongSerializer", LONG)
    override fun <OUT> double(mapping: BiDiMapping<Double, OUT>) = adapter(mapping, Decoder::decodeDouble, Encoder::encodeDouble, "DoubleSerializer", DOUBLE)
    override fun <OUT> text(mapping: BiDiMapping<String, OUT>) = adapter(mapping, Decoder::decodeString, Encoder::encodeString, "TextSerializer", STRING)
    override fun <OUT> bigInteger(mapping: BiDiMapping<BigInteger, OUT>) = throw UnsupportedOperationException("kotlinx.serialization does not support BigInteger.")
    override fun <OUT> bigDecimal(mapping: BiDiMapping<BigDecimal, OUT>) = throw UnsupportedOperationException("kotlinx.serialization does not support BigDecimal.")

    private fun <IN, OUT> adapter(mapping: BiDiMapping<IN, OUT>, decode: Decoder.() -> IN, encode: Encoder.(IN) -> Unit, serialName: String, kind: PrimitiveKind): AutoMappingConfiguration<JsonBuilder> =
        apply {
            @Suppress("UNCHECKED_CAST")
            val serializer = object : KSerializer<Any> {
                override val descriptor = PrimitiveSerialDescriptor(serialName, kind)

                override fun deserialize(decoder: Decoder) = mapping.invoke(decoder.decode()) as Any

                override fun serialize(encoder: Encoder, value: Any) {
                    encoder.encode(mapping(value as OUT))
                }

            }
            this@asConfigurable.serializersModule += (SerializersModule { contextual(Reflection.getOrCreateKotlinClass(mapping.clazz), serializer) })
        }

    override fun done(): JsonBuilder = this@asConfigurable
}

