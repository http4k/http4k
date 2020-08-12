package org.http4k.format

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.BiDiMapping
import org.http4k.lens.BiDiWsMessageLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.ContentNegotiation.Companion.None
import org.http4k.lens.string
import org.http4k.websocket.WsMessage
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

open class ConfigurableMoshi(builder: Moshi.Builder) : AutoMarshalling() {
open class ConfigurableMoshi(builder: Moshi.Builder,
                             val defaultContentType: ContentType = APPLICATION_JSON) : AutoMarshallingJson() {

    private val moshi: Moshi = builder.build()

    override fun asFormatString(input: Any): String = moshi.adapter(input.javaClass).toJson(input)

    fun <T : Any> asJsonString(t: T, c: KClass<T>): String = moshi.adapter(c.java).toJson(t)

    override fun <T : Any> asA(input: String, target: KClass<T>): T = moshi.adapter(target.java).fromJson(input)!!

    inline fun <reified T : Any> Body.Companion.auto(description: String? = null,
                                                     contentNegotiation: ContentNegotiation = None,
                                                     contentType: ContentType = defaultContentType): BiDiBodyLensSpec<T> =
        Body.string(contentType, description, contentNegotiation).map({ asA(it, T::class) }, { asFormatString(it) })

    inline fun <reified T : Any> WsMessage.Companion.auto(): BiDiWsMessageLensSpec<T> = WsMessage.string().map({ it.asA(T::class) }, { asFormatString(it) })
}

fun Moshi.Builder.asConfigurable() = object : AutoMappingConfiguration<Moshi.Builder> {
    override fun <OUT> int(mapping: BiDiMapping<Int, OUT>) = adapter(mapping, { value(it) }, { nextInt() })
    override fun <OUT> long(mapping: BiDiMapping<Long, OUT>) = adapter(mapping, { value(it) }, { nextLong() })
    override fun <OUT> double(mapping: BiDiMapping<Double, OUT>) = adapter(mapping, { value(it) }, { nextDouble() })
    override fun <OUT> bigInteger(mapping: BiDiMapping<BigInteger, OUT>) = adapter(mapping, { value(it) }, { nextLong().toBigInteger() })
    override fun <OUT> bigDecimal(mapping: BiDiMapping<BigDecimal, OUT>) = adapter(mapping, { value(it) }, { nextDouble().toBigDecimal() })
    override fun <OUT> boolean(mapping: BiDiMapping<Boolean, OUT>) = adapter(mapping, { value(it) }, { nextBoolean() })
    override fun <OUT> text(mapping: BiDiMapping<String, OUT>) = adapter(mapping, { value(it) }, { nextString() })

    private fun <IN, OUT> adapter(mapping: BiDiMapping<IN, OUT>, write: JsonWriter.(IN) -> Unit, read: JsonReader.() -> IN) =
        apply {
            add(mapping.clazz, object : JsonAdapter<OUT>() {
                override fun fromJson(reader: JsonReader) = mapping.invoke(reader.read())

                override fun toJson(writer: JsonWriter, value: OUT?) {
                    value?.let { writer.write(mapping(it)) } ?: writer.nullValue()
                }
            })
        }

    // add the Kotlin adapter last, as it will hjiack our custom mappings otherwise
    override fun done() = this@asConfigurable.add(KotlinJsonAdapterFactory()).add(Unit::class.java, UnitAdapter)
}

private object UnitAdapter : JsonAdapter<Unit>() {
    override fun fromJson(reader: JsonReader) {
        reader.readJsonValue(); Unit
    }

    override fun toJson(writer: JsonWriter, value: Unit?) {
        value?.let { writer.beginObject().endObject() } ?: writer.nullValue()
    }
}
