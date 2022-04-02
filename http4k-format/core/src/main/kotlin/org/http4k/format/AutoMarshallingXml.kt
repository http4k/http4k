package org.http4k.format

import org.http4k.asString
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.BiDiWsMessageLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.lens.httpBodyRoot
import org.http4k.lens.string
import org.http4k.websocket.WsMessage

abstract class AutoMarshallingXml : AutoMarshalling() {

    inline fun <reified T : Any> String.asA(): T = asA(this)

    abstract fun Any.asXmlString(): String

    override fun asFormatString(input: Any): String = input.asXmlString()

    inline fun <reified T : Any> Body.Companion.auto(description: String? = null, contentNegotiation: ContentNegotiation = ContentNegotiation.None, contentType: ContentType = APPLICATION_XML): BiDiBodyLensSpec<T> =
        httpBodyRoot(listOf(Meta(true, "body", ObjectParam, "body", description)), contentType, contentNegotiation)
            .map({ it.payload.asString() }, { Body(it) })
            .map({ it.asA<T>() }, { it.asXmlString() })

    inline fun <reified T : Any> WsMessage.Companion.auto(): BiDiWsMessageLensSpec<T> = WsMessage.string().map({ it.asA<T>() }, { it.asXmlString() })
}
