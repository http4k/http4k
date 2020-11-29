package org.http4k.format

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.BiDiWsMessageLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.string
import org.http4k.websocket.WsMessage
import kotlin.reflect.KClass
import com.beust.klaxon.Klaxon as KKlaxon

open class ConfigurableKlaxon(private val klaxon: KKlaxon,
                              val defaultContentType: ContentType = APPLICATION_JSON) : AutoMarshallingJson() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> asA(input: String, target: KClass<T>) =
        klaxon.fromJsonObject(klaxon.parseJsonObject(input.reader()), target.java, target) as T

    override fun asFormatString(input: Any) =
        klaxon.toJsonString(input)

    inline fun <reified T : Any> Body.Companion.auto(description: String? = null,
                                                     contentNegotiation: ContentNegotiation = ContentNegotiation.None,
                                                     contentType: ContentType = defaultContentType): BiDiBodyLensSpec<T> =
        Body.string(contentType, description, contentNegotiation).map({ asA(it, T::class) }, { asFormatString(it) })

    inline fun <reified T : Any> WsMessage.Companion.auto(): BiDiWsMessageLensSpec<T> = WsMessage.string().map({ it.asA(T::class) }, { asFormatString(it) })
}

fun KKlaxon.asConfigurable() = asConfigurable(KKlaxon())
