package org.http4k.format

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Uri.Companion
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.BiDiWsMessageLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.ContentNegotiation.Companion.None
import org.http4k.lens.string
import org.http4k.websocket.WsMessage
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
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
        .add(custom({ LocalTime.parse(it, DateTimeFormatter.ISO_LOCAL_TIME) }, DateTimeFormatter.ISO_LOCAL_TIME::format))
        .add(custom({ LocalDate.parse(it, DateTimeFormatter.ISO_DATE) }, DateTimeFormatter.ISO_DATE::format))
        .add(custom({ LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }, DateTimeFormatter.ISO_LOCAL_DATE_TIME::format))
        .add(custom({ ZonedDateTime.parse(it, DateTimeFormatter.ISO_ZONED_DATE_TIME) }, DateTimeFormatter.ISO_ZONED_DATE_TIME::format))
        .add(custom(Companion::of))
        .add(custom(::URL, URL::toExternalForm))
        .add(custom(UUID::fromString))
        .add(custom(::Regex, Regex::pattern))
        .add(KotlinJsonAdapterFactory()))

private inline fun <T> custom(crossinline readFn: (String) -> T, crossinline writeFn: (T) -> String = { it.toString() }) =
        object {
            @FromJson
            fun read(t: String): T = readFn(t)

            @ToJson
            fun write(t: T): String = writeFn(t)
        }