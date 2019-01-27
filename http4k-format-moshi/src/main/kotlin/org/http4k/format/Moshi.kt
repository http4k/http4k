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

    inline fun <reified T : Any> Body.Companion.auto(description: String? = null, contentNegotiation: ContentNegotiation = None): BiDiBodyLensSpec<T> =
        Body.string(APPLICATION_JSON, description, contentNegotiation).map({ asA(it, T::class) }, { asJsonString(it) })

    inline fun <reified T : Any> WsMessage.Companion.auto(): BiDiWsMessageLensSpec<T> = WsMessage.string().map({ it.asA(T::class) }, { asJsonString(it) })
}

object Moshi : ConfigurableMoshi(ConfigureMoshi().withStandardMappings())

class ConfigureMoshi : ConfigureAutoMarshallingJson<Builder> {
    private val builder = Moshi.Builder().add(KotlinJsonAdapterFactory())

    override fun <T> text(mapping: BiDiMapping<String, T>) {
        println("registering " + mapping.clazz)
        builder.add(mapping.clazz, object : JsonAdapter<T>() {
            override fun fromJson(reader: JsonReader) = mapping.read(reader.nextString())

            override fun toJson(writer: JsonWriter, value: T?) {
                Thread.dumpStack()
                println(mapping.clazz)
                value?.let { writer.value(mapping.write(it)) } ?: writer.nullValue()
            }
        })
    }

    override fun done(): Builder = builder
}