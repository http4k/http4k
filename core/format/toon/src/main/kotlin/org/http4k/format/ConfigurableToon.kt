package org.http4k.format

import dev.toonformat.jtoon.DecodeOptions
import dev.toonformat.jtoon.EncodeOptions
import dev.toonformat.jtoon.JToon
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.Text
import org.http4k.core.HttpMessage
import org.http4k.core.with
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.ContentNegotiation.Companion.None
import org.http4k.lens.string
import java.io.InputStream
import kotlin.reflect.KClass

open class ConfigurableToon(
    builder: ToonBuilder,
    private val encodeOptions: EncodeOptions,
    private val decodeOptions: DecodeOptions
) : AutoMarshalling() {

    private val moshi = builder.build()

    override val defaultContentType = Text("text/toon")

    override fun <T : Any> asA(input: String, target: KClass<T>) =
        moshi.asA(JToon.decodeToJson(input, decodeOptions), target)

    override fun <T : Any> asA(input: InputStream, target: KClass<T>): T = asA(input.reader().readText(), target)

    override fun asFormatString(input: Any): String = JToon.encodeJson(moshi.asFormatString(input), encodeOptions)

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

    /**
     * Convenience function to write the object as Toon to the message body and set the content type.
     */
    inline fun <reified T : Any, R : HttpMessage> R.toon(t: T): R = with(Body.auto<T>().toLens() of t)

    /**
     * Convenience function to read an object as Toon from the message body.
     */
    inline fun <reified T : Any> HttpMessage.toon(): T = Body.auto<T>().toLens()(this)

}
