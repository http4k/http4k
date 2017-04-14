package org.reekwest.http.core.contract

import org.reekwest.http.core.*
import org.reekwest.http.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.reekwest.http.core.body.string
import org.reekwest.http.core.contract.Header.Common.CONTENT_TYPE
import java.nio.ByteBuffer

open class BodySpec<OUT : Any>(private val delegate: LensSpec<HttpMessage, OUT>) {
    fun <NEXT : Any> map(nextIn: (OUT) -> NEXT): BodySpec<NEXT> = BodySpec(delegate.map(nextIn))

    fun <NEXT : Any> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT): BodySpec<NEXT> =
        BodySpec(delegate.map(nextIn, nextOut))

    fun required(description: String? = null) = delegate.required("body", description)
}

object Body : BodySpec<ByteBuffer>(LensSpec<HttpMessage, ByteBuffer>(
    "body",
    { target, _ -> listOf(target.body) },
    { target, _, bodies -> bodies.fold(target) { a, b -> a.copy(body = b) } }, { it }, { it })

) {
    val string = Body.map { it: ByteBuffer -> String(it.array()) }
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
