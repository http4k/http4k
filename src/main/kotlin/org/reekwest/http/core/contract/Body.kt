package org.reekwest.http.core.contract

import org.reekwest.http.core.*
import org.reekwest.http.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.reekwest.http.core.body.string
import org.reekwest.http.core.contract.Body.locator
import org.reekwest.http.core.contract.Header.Common.CONTENT_TYPE
import java.nio.ByteBuffer

open class BodySpec<OUT : Any>(private val delegate: LensSpec<HttpMessage, OUT>) {
    fun <NEXT : Any> map(nextIn: (OUT) -> NEXT): BodySpec<NEXT> = BodySpec(delegate.map(nextIn))

    fun <NEXT : Any> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT): BodySpec<NEXT> =
        BodySpec(delegate.map(nextIn, nextOut))

    fun required(description: String? = null) = delegate.required("body", description)
}

object Body : BodySpec<ByteBuffer>(LensSpec(locator, { it }, { it })) {
    val string = Body.map({ it: ByteBuffer -> String(it.array()) }, { it.toByteBuffer() })

    internal val locator = object : Locator<HttpMessage, ByteBuffer> {
        override val name = "body"
        override fun get(target: HttpMessage, name: String) = listOf(target.body)
        override fun set(target: HttpMessage, name: String, values: List<ByteBuffer>) = values.fold(target) { a, b -> a.copy(body = b) }
    }
}

/**
 * Extension functions for various body types
 */

fun Body.string(description: String? = null) = Body.string.required(description)

fun Body.form() = StringLensSpec<Request>("form", {
    target, _ ->
    if (CONTENT_TYPE(target) != APPLICATION_FORM_URLENCODED) throw Invalid(Meta("form", "body", true))
    else listOf(target.body.string())
},
    { target, _, formBody -> formBody.fold(target, { memo, next -> memo.body(next.toByteBuffer()) }) }
).map { it.toParameters() }.required("body")
