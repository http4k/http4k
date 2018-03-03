package org.http4k.format

import com.jsoniter.JsonIterator
import com.jsoniter.ValueType
import com.jsoniter.output.JsonStream
import com.jsoniter.spi.Config
import org.http4k.core.Body
import org.http4k.format.JsonLibAutoMarshallingJson
import org.http4k.format.JsonType
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.BiDiWsMessageLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.websocket.WsMessage
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

typealias JsonAny = com.jsoniter.any.Any

class InvalidJsonException : Exception("Could not convert to a JSON Object or Array")

open class ConfigurableJsonIter(config: Config) : JsonLibAutoMarshallingJson<JsonAny>() {
    override fun typeOf(value: JsonAny): JsonType =
            when (value.valueType()) {
                ValueType.ARRAY -> JsonType.Array
                ValueType.NULL -> JsonType.Null
                ValueType.OBJECT -> JsonType.Object
                ValueType.BOOLEAN ->  JsonType.Boolean
                ValueType.NUMBER ->  JsonType.Number
                ValueType.STRING ->  JsonType.String
                ValueType.INVALID
                -> throw IllegalArgumentException("Don't know now to translate '$value'")
            }

//    private val builder = Config.Builder(config).build()
    // TODO private val pretty = builder.setPrettyPrinting().build()

    // TODO Use JsonIterator.parse() instead ?
    override fun String.asJsonObject(): JsonAny = JsonIterator.deserialize(this).also {
        if(it.valueType() == ValueType.INVALID) throw InvalidJsonException()
    }
    override fun String?.asJsonValue(): JsonAny = this?.let { JsonAny.wrap(this) } ?: JsonAny.wrapNull()
    override fun Int?.asJsonValue(): JsonAny = this?.let { JsonAny.wrap(this) } ?: JsonAny.wrapNull()
    override fun Double?.asJsonValue(): JsonAny = this?.let { JsonAny.wrap(BigDecimal(this)) } ?: JsonAny.wrapNull()
    override fun Long?.asJsonValue(): JsonAny = this?.let { JsonAny.wrap(this) } ?: JsonAny.wrapNull()
    override fun BigDecimal?.asJsonValue(): JsonAny = this?.let { JsonAny.wrap(this) } ?: JsonAny.wrapNull()
    override fun BigInteger?.asJsonValue(): JsonAny = this?.let { JsonAny.wrap(this) } ?: JsonAny.wrapNull()
    override fun Boolean?.asJsonValue(): JsonAny = this?.let { JsonAny.wrap(this) } ?: JsonAny.wrapNull()
    override fun <T : Iterable<JsonAny>> T.asJsonArray(): JsonAny = this.fold(mutableListOf<JsonAny>()) { memo, o -> memo.add(o); memo }
            .let { JsonAny.rewrap(it) }

    override fun JsonAny.asPrettyJsonString(): String = JsonStream.serialize(this) // TODO
    override fun JsonAny.asCompactJsonString(): String = JsonStream.serialize(this) // TODO
    override fun <LIST : Iterable<Pair<String, JsonAny>>> LIST.asJsonObject(): JsonAny {
        val root = mutableMapOf<String, JsonAny>()
        this.forEach { root[it.first] = it.second }
        return JsonAny.wrap(root)
    }

    override fun fields(node: JsonAny): Iterable<Pair<String, JsonAny>> {
        val fieldList = mutableListOf<Pair<String, JsonAny>>()
        val entries = node.entries()
        while (entries.next()) {
            fieldList += entries.key() to entries.value()
        }
        return fieldList
    }

    override fun elements(value: JsonAny): Iterable<JsonAny> = value.asJsonArray()
    override fun text(value: JsonAny): String = value.asPrettyJsonString()

    override fun asJsonObject(a: Any): JsonAny = a?.let {
        when (a) {
            is Int -> JsonAny.wrap((it as java.lang.Integer).toInt())
            is Long -> JsonAny.wrap((it as java.lang.Long).toLong())
            is Float -> JsonAny.wrap((it as java.lang.Float).toFloat())
            is Double -> JsonAny.wrap((it as java.lang.Double).toDouble())
            is String -> JsonAny.wrap((it as java.lang.String)) // FIXME it should call wrap(String) but calls wrap(Object)
// TODO     Collection<T>, List<T>, Map<String, T>
            else -> JsonAny.wrap(a)
        } ?: JsonAny.wrapNull()

    }
    override fun <T : Any> asA(s: String, c: KClass<T>): T = JsonIterator.deserialize<T>(s, c.java)
    override fun <T : Any> asA(j: JsonAny, c: KClass<T>): T = j.`as`(c.java)

    inline fun <reified T : Any> String.asA(): T = asA(this, T::class)
    inline fun <reified T : Any> JsonAny.asA(): T = asA(this, T::class)

    inline fun <reified T : Any> Body.Companion.auto(description: String? = null, contentNegotiation: ContentNegotiation = ContentNegotiation.None): BiDiBodyLensSpec<T> = Body.json(description, contentNegotiation).map({ it.asA<T>() }, { it.asJsonObject() })

    inline fun <reified T : Any> WsMessage.Companion.auto(): BiDiWsMessageLensSpec<T> = WsMessage.json().map({ it.asA<T>() }, { it.asJsonObject() })
}

object JsonIter : ConfigurableJsonIter(Config.Builder().build())
