package org.http4k.format

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.BiDiMapping
import org.http4k.lens.BiDiWsMessageLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.string
import org.http4k.websocket.WsMessage
import kotlin.reflect.KClass

open class ConfigurableMoshi(builder: Moshi.Builder) : AutoMarshallingJson() {

    private val moshi: Moshi = builder.build()

    private fun <T> adapterFor(c: Class<T>): JsonAdapter<T> = moshi.adapter(c).failOnUnknown()

    override fun asJsonString(a: Any): String = adapterFor(a.javaClass).toJson(a)

    fun <T : Any> asJsonString(t: T, c: KClass<T>): String = adapterFor(c.java).toJson(t)

    override fun <T : Any> asA(s: String, c: KClass<T>): T = adapterFor(c.java).fromJson(s)!!

    inline fun <reified T : Any> asA(s: String): T = asA(s, T::class)

    inline fun <reified T : Any> Body.Companion.auto(description: String? = null, contentNegotiation: ContentNegotiation = ContentNegotiation.None): BiDiBodyLensSpec<T> =
        Body.string(ContentType.APPLICATION_JSON, description, contentNegotiation).map({ asA(it, T::class) }, { asJsonString(it) })

    inline fun <reified T : Any> WsMessage.Companion.auto(): BiDiWsMessageLensSpec<T> = WsMessage.string().map({ it.asA(T::class) }, { asJsonString(it) })
}

fun <T> Moshi.Builder.text(mapping: BiDiMapping<String, T>) {
    mapping.apply {
        add(clazz, object : JsonAdapter<T>() {
            override fun fromJson(reader: JsonReader) = read(reader.nextString())

            override fun toJson(writer: JsonWriter, value: T?) {
                value?.let { writer.value(write(it)) } ?: writer.nullValue()
            }
        })
    }
}