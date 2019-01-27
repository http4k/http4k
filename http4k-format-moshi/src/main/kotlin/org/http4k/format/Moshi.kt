package org.http4k.format

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Moshi.Builder
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.BiDiMapping
import org.http4k.lens.BiDiWsMessageLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.ContentNegotiation.Companion.None
import org.http4k.lens.duration
import org.http4k.lens.instant
import org.http4k.lens.localDate
import org.http4k.lens.localDateTime
import org.http4k.lens.localTime
import org.http4k.lens.offsetDateTime
import org.http4k.lens.offsetTime
import org.http4k.lens.regexObject
import org.http4k.lens.string
import org.http4k.lens.uri
import org.http4k.lens.url
import org.http4k.lens.uuid
import org.http4k.lens.zonedDateTime
import org.http4k.websocket.WsMessage
import kotlin.reflect.KClass


open class ConfigurableMoshi(builder: Moshi.Builder) : AutoMarshallingJson() {

    private val moshi: Moshi = builder.build()

    private fun <T> adapterFor(c: Class<T>): JsonAdapter<T> = moshi.adapter(c).failOnUnknown()

    override fun asJsonString(a: Any): String = adapterFor(a.javaClass).toJson(a)

    fun <T : Any> asJsonString(t: T, c: KClass<T>): String = adapterFor(c.java).toJson(t)

    override fun <T : Any> asA(s: String, c: KClass<T>): T = adapterFor(c.java).fromJson(s)!!

    inline fun <reified T : Any> asA(s: String): T = asA(s, T::class)

    inline fun <reified T : Any> Body.Companion.auto(description: String? = null, contentNegotiation: ContentNegotiation = None): BiDiBodyLensSpec<T> =
        Body.string(APPLICATION_JSON, description, contentNegotiation).map({ asA(it, T::class) }, { asJsonString(it) })

    inline fun <reified T : Any> WsMessage.Companion.auto(): BiDiWsMessageLensSpec<T> = WsMessage.string().map({ it.asA(T::class) }, { asJsonString(it) })
}

object Moshi : ConfigurableMoshi(Moshi.Builder()
    .custom(BiDiMapping.duration())
    .custom(BiDiMapping.uri())
    .custom(BiDiMapping.url())
    .custom(BiDiMapping.uuid())
    .custom(BiDiMapping.regexObject())
    .custom(BiDiMapping.instant())
    .custom(BiDiMapping.localTime())
    .custom(BiDiMapping.localDate())
    .custom(BiDiMapping.localDateTime())
    .custom(BiDiMapping.zonedDateTime())
    .custom(BiDiMapping.offsetTime())
    .custom(BiDiMapping.offsetDateTime())
    .add(KotlinJsonAdapterFactory()))

inline fun <reified T> Builder.custom(mapping: BiDiMapping<T>): Builder = add(T::class.java, object : JsonAdapter<T>() {
    override fun fromJson(reader: JsonReader) = mapping.read(reader.nextString())

    override fun toJson(writer: JsonWriter, value: T?) {
        value?.let { writer.value(mapping.write(it)) } ?: writer.nullValue()
    }
})