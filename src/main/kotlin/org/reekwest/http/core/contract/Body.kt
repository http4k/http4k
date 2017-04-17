package org.reekwest.http.core.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.copy
import java.nio.ByteBuffer

open class BodySpec<OUT : Any>(private val delegate: LensSpec<HttpMessage, OUT>) {
    fun <NEXT : Any> map(nextIn: (OUT) -> NEXT): BodySpec<NEXT> = BodySpec(delegate.map(nextIn))

    fun <NEXT : Any> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT): BodySpec<NEXT> =
        BodySpec(delegate.map(nextIn, nextOut))

    fun required(description: String? = null) = delegate.required("body", description)
}

internal object BodyLocator : Locator<HttpMessage, ByteBuffer> {
    override val location = "body"
    override fun get(target: HttpMessage, name: String) = listOf(target.body)
    override fun set(target: HttpMessage, name: String, values: List<ByteBuffer>) = values.fold(target) { a, b -> a.copy(body = b) }
}


object Body : BodySpec<ByteBuffer>(LensSpec(BodyLocator, { it }, { it })) {
    val string = Body.map({ it: ByteBuffer -> String(it.array()) }, { it.asByteBuffer() })

    fun binary(description: String? = null) = Body.required(description)
    fun string(description: String? = null) = Body.string.required(description)

}

/**
 * Extension functions for various body types
 */
