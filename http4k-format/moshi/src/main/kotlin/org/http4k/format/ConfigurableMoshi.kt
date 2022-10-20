package org.http4k.format

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.buffer
import okio.source
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.format.StrictnessMode.*
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.BiDiMapping
import org.http4k.lens.BiDiWsMessageLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.ContentNegotiation.Companion.None
import org.http4k.lens.string
import org.http4k.websocket.WsMessage
import java.io.InputStream
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

open class ConfigurableMoshi(
    builder: Moshi.Builder,
    val defaultContentType: ContentType = APPLICATION_JSON,
    private val strictness: StrictnessMode = Lenient
) : AutoMarshallingJson<MoshiNode>() {

    private val moshi = builder.build()

    private val objectAdapter = moshi.adapter(Any::class.java)

    override fun MoshiNode.asPrettyJsonString(): String = objectAdapter.indent("    ").toJson(unwrap())
    override fun MoshiNode.asCompactJsonString(): String = objectAdapter.toJson(unwrap())

    override fun String.asJsonObject() = MoshiNode.wrap(objectAdapter.fromJson(this))

    override fun <LIST : Iterable<Pair<String, MoshiNode>>> LIST.asJsonObject() = MoshiObject(toMap())
    override fun String?.asJsonValue() = if (this == null) MoshiNull else MoshiString(this)
    override fun Int?.asJsonValue() = if (this == null) MoshiNull else MoshiInteger(toLong())
    override fun Double?.asJsonValue() = if (this == null) MoshiNull else MoshiDecimal(this)
    override fun Long?.asJsonValue() = if (this == null) MoshiNull else MoshiInteger(this)
    override fun BigDecimal?.asJsonValue() = if (this == null) MoshiNull else MoshiDecimal(toDouble())
    override fun BigInteger?.asJsonValue() = if (this == null) MoshiNull else MoshiInteger(toLong())
    override fun Boolean?.asJsonValue() = if (this == null) MoshiNull else MoshiBoolean(this)
    override fun <T : Iterable<MoshiNode>> T.asJsonArray() = MoshiArray(toList())

    override fun textValueOf(node: MoshiNode, name: String): String? = (node as? MoshiObject)
        ?.attributes?.get(name)
        ?.unwrap()?.toString()

    override fun decimal(value: MoshiNode) = (value as MoshiDecimal).value.toBigDecimal()
    override fun integer(value: MoshiNode) = ((value as MoshiInteger).value)
    override fun bool(value: MoshiNode) = (value as MoshiBoolean).value
    override fun text(value: MoshiNode) = (value as MoshiString).value
    override fun elements(value: MoshiNode) = (value as MoshiArray).elements
    override fun fields(node: MoshiNode) = (node as? MoshiObject)
        ?.attributes
        ?.map { it.key to it.value }
        ?: emptyList()

    override fun typeOf(value: MoshiNode) = when (value) {
        is MoshiNull -> JsonType.Null
        is MoshiObject -> JsonType.Object
        is MoshiArray -> JsonType.Array
        is MoshiInteger -> JsonType.Integer
        is MoshiDecimal -> JsonType.Number
        is MoshiString -> JsonType.String
        is MoshiBoolean -> JsonType.Boolean
    }

    override fun asFormatString(input: Any): String = moshi.adapter(input.javaClass).toJson(input)

    fun <T : Any> asJsonString(t: T, c: KClass<T>): String = moshi.adapter(c.java).toJson(t)

    override fun <T : Any> asA(input: String, target: KClass<T>): T = adapterFor(target).fromJson(input)!!

    private fun <T : Any> adapterFor(target: KClass<T>) = when (strictness) {
        Lenient -> moshi.adapter(target.java)
        FailOnUnknown -> moshi.adapter(target.java).failOnUnknown()
    }

    override fun <T : Any> asA(input: InputStream, target: KClass<T>): T = adapterFor(target).fromJson(
        input.source().buffer()
    )!!

    override fun asJsonObject(input: Any): MoshiNode = MoshiNode.wrap(objectAdapter.toJsonValue(input))

    override fun <T : Any> asA(j: MoshiNode, target: KClass<T>): T = adapterFor(target)
        .fromJsonValue(j.unwrap())!!

    inline fun <reified T : Any> Body.Companion.auto(
        description: String? = null,
        contentNegotiation: ContentNegotiation = None,
        contentType: ContentType = defaultContentType
    ): BiDiBodyLensSpec<T> =
        autoBody(description, contentNegotiation, contentType)

    inline fun <reified T : Any> autoBody(
        description: String? = null,
        contentNegotiation: ContentNegotiation = None,
        contentType: ContentType = defaultContentType
    ): BiDiBodyLensSpec<T> =
        Body.string(contentType, description, contentNegotiation).map({ asA(it, T::class) }, {
            asFormatString(it)
        })

    inline fun <reified T : Any> WsMessage.Companion.auto(): BiDiWsMessageLensSpec<T> =
        WsMessage.string().map({ it.asA(T::class) }, { asFormatString(it) })
}

fun Moshi.Builder.asConfigurable(
    kotlinFactory: JsonAdapter.Factory = KotlinJsonAdapterFactory()
) = object : AutoMappingConfiguration<Moshi.Builder> {
    override fun <OUT> int(mapping: BiDiMapping<Int, OUT>) = adapter(mapping, { value(it) }, { nextInt() })
    override fun <OUT> long(mapping: BiDiMapping<Long, OUT>) =
        adapter(mapping, { value(it) }, { nextLong() })

    override fun <OUT> double(mapping: BiDiMapping<Double, OUT>) =
        adapter(mapping, { value(it) }, { nextDouble() })

    override fun <OUT> bigInteger(mapping: BiDiMapping<BigInteger, OUT>) =
        adapter(mapping, { value(it) }, { nextLong().toBigInteger() })

    override fun <OUT> bigDecimal(mapping: BiDiMapping<BigDecimal, OUT>) =
        adapter(mapping, { value(it) }, { nextDouble().toBigDecimal() })

    override fun <OUT> boolean(mapping: BiDiMapping<Boolean, OUT>) =
        adapter(mapping, { value(it) }, { nextBoolean() })

    override fun <OUT> text(mapping: BiDiMapping<String, OUT>) =
        adapter(mapping, { value(it) }, { nextString() })

    private fun <IN, OUT> adapter(
        mapping: BiDiMapping<IN, OUT>,
        write: JsonWriter.(IN) -> Unit,
        read: JsonReader.() -> IN
    ) =
        apply {
            add(mapping.clazz, object : JsonAdapter<OUT>() {
                override fun fromJson(reader: JsonReader) = mapping.invoke(reader.read())

                override fun toJson(writer: JsonWriter, value: OUT?) {
                    value?.let { writer.write(mapping(it)) } ?: writer.nullValue()
                }
            }.nullSafe())
        }

    // add the Kotlin adapter last, as it will hjiack our custom mappings otherwise
    override fun done() =
        this@asConfigurable.add(kotlinFactory).add(Unit::class.java, UnitAdapter)
}

private object UnitAdapter : JsonAdapter<Unit>() {
    override fun fromJson(reader: JsonReader) {
        reader.readJsonValue(); Unit
    }

    override fun toJson(writer: JsonWriter, value: Unit?) {
        value?.let { writer.beginObject().endObject() } ?: writer.nullValue()
    }
}
