package org.http4k.format

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.BiDiWsMessageLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.ContentNegotiation.Companion.None
import org.http4k.lens.string
import org.http4k.websocket.WsMessage
import kotlin.reflect.KClass

open class ConfigurableMoshi(builder: Moshi.Builder) : AutoMarshallingJson() {

    private val moshi: Moshi = builder.build()

    private fun <T : Any> adapterFor(c: Class<T>): JsonAdapter<T> = moshi.adapter(c).failOnUnknown()

    override fun asJsonString(a: Any): String = adapterFor(a.javaClass).toJson(a)

    fun <T : Any> asJsonString(t: T, c: KClass<T>): String = adapterFor(c.java).toJson(t)

    override fun <T : Any> asA(s: String, c: KClass<T>): T = adapterFor(c.java).fromJson(s)!!

    inline fun <reified T : Any> asA(s: String): T = asA(s, T::class)

    inline fun <reified T : Any> Body.Companion.auto(description: String? = null, contentNegotiation: ContentNegotiation = None): BiDiBodyLensSpec<T> =
        Body.string(APPLICATION_JSON, description, contentNegotiation).map({ asA(it, T::class) }, { asJsonString(it) })

    inline fun <reified T : Any> WsMessage.Companion.auto(): BiDiWsMessageLensSpec<T> = WsMessage.string().map({ it.asA(T::class) }, { asJsonString(it) })
}

object Moshi : ConfigurableMoshi(Moshi.Builder())
