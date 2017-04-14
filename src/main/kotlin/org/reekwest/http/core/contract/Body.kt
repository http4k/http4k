package org.reekwest.http.core.contract

import org.reekwest.http.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Request
import org.reekwest.http.core.body
import org.reekwest.http.core.body.string
import org.reekwest.http.core.contract.Header.Common.CONTENT_TYPE
import org.reekwest.http.core.toParameters
import java.nio.ByteBuffer

open class BodySpec<OUT: Any>(private val delegate: LensSpec<HttpMessage, OUT>) {
    fun <NEXT : Any> map(nextIn: (OUT) -> NEXT): BodySpec<NEXT> = BodySpec(delegate.map(nextIn))

    fun <NEXT : Any> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT): BodySpec<NEXT> =
        BodySpec(delegate.map(nextIn, nextOut))

    fun get(description: String? = null) = delegate.required("body", description)
}

object Body : BodySpec<ByteBuffer>(LensSpec<HttpMessage, ByteBuffer>(
    "body",
    { target, _ -> listOf(target.body) },
    { target, _, bytes -> target }, { it }, { it })
)

/**
 * Extension functions for various body types
 */

fun Body.string(description: String? = null) = Body.map { String(it.array()) }.get(description)

fun Body.form() = StringLensSpec<Request>("form", {
    message, _ ->
    if (CONTENT_TYPE(message) != APPLICATION_FORM_URLENCODED) throw Invalid(Meta("form", "body"))
    else listOf(message.body.string())
},
    { msg, _, formBody -> formBody.fold(msg, { memo, next -> memo.body(next.toByteBuffer()) }) }
).map { it.toParameters() }.required("body")
