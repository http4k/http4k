@file:Suppress("UNCHECKED_CAST")

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

open class ConfigurableMoshi(builder: Moshi.Builder) : AutoMarshallingJson<Map<*, *>>() {

    private val moshi = builder.build()

    override fun asJsonString(a: Any): String = moshi.adapter(a.javaClass).failOnUnknown().toJson(a)

    private val mapAdapter = moshi.adapter(Map::class.java)

    override fun asJsonObject(a: Any): Map<*, *> = mapAdapter.fromJson((moshi.adapter<Any>(a::class.java).failOnUnknown() as JsonAdapter<Any>).toJson(a))!!

    override fun parse(s: String): Map<*, *> = mapAdapter.fromJson(s)!!

    override fun <T : Any> asA(s: String, c: KClass<T>): T = (moshi.adapter(c.java).failOnUnknown() as JsonAdapter<T>).fromJson(s)!!
    override fun <T : Any> asA(j: Map<*, *>, c: KClass<T>): T = moshi.adapter(c.java).fromJson(mapAdapter.toJson(j))!!

    inline fun <reified T : Any> Body.Companion.auto(description: String? = null, contentNegotiation: ContentNegotiation = None): BiDiBodyLensSpec<T> =
        Body.string(APPLICATION_JSON, description, contentNegotiation).map({ asA(it, T::class) }, { asJsonString(it) })

    inline fun <reified T : Any> WsMessage.Companion.auto(): BiDiWsMessageLensSpec<T> = WsMessage.string().map({ it.asA(T::class) }, { asJsonString(it) })
}

object Moshi : ConfigurableMoshi(Moshi.Builder())
