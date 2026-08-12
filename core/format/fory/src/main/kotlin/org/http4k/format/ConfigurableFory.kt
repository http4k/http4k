package org.http4k.format

import org.apache.fory.ThreadSafeFory
import org.http4k.asByteBuffer
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.HttpMessage
import org.http4k.core.with
import org.http4k.lens.BiDiBodyLensSpec
import org.http4k.lens.BiDiWsMessageLensSpec
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.ContentNegotiation.Companion.None
import org.http4k.lens.binary
import org.http4k.websocket.WsMessage
import java.io.InputStream
import kotlin.reflect.KClass

open class ConfigurableFory(private val fory: ThreadSafeFory) {

    open val defaultContentType = OCTET_STREAM

    fun asBytes(input: Any): ByteArray = fory.serialize(input)

    fun <T : Any> asA(input: ByteArray, target: KClass<T>): T = fory.deserialize(input, target.java)

    inline fun <reified T : Any> asA(input: InputStream): T = asA(input.readAllBytes())
    inline fun <reified T : Any> asA(input: ByteArray): T = asA(input, T::class)

    inline fun <reified T : Any> Body.Companion.auto(
        description: String? = null,
        contentNegotiation: ContentNegotiation = None,
        contentType: ContentType = defaultContentType
    ): BiDiBodyLensSpec<T> = autoBody(description, contentNegotiation, contentType)

    inline fun <reified T : Any> autoBody(
        description: String? = null,
        contentNegotiation: ContentNegotiation = None,
        contentType: ContentType = defaultContentType
    ): BiDiBodyLensSpec<T> = Body.binary(contentType, description, contentNegotiation)
        .map({ asA<T>(it) }, { asBytes(it).inputStream() })

    inline fun <reified T : Any> WsMessage.Companion.auto(): BiDiWsMessageLensSpec<T> =
        binary().map({ asA<T>(it.array()) }, { asBytes(it).asByteBuffer() })

    /**
     * Convenience function to write the object as Fory to the message body and set the content type.
     */
    inline fun <reified T : Any, R : HttpMessage> R.binary(t: T): R = with(Body.auto<T>().toLens() of t)

    /**
     * Convenience function to read an object as Fory from the message body.
     */
    inline fun <reified T : Any> HttpMessage.binary(): T = Body.auto<T>().toLens()(this)
}

