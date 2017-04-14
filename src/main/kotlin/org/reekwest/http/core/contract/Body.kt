package org.reekwest.http.core.contract

import org.reekwest.http.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Request
import org.reekwest.http.core.body
import org.reekwest.http.core.body.string
import org.reekwest.http.core.contract.Header.Common.CONTENT_TYPE
import org.reekwest.http.core.toParameters
import java.nio.ByteBuffer

object Body : LensSpec<HttpMessage, ByteBuffer>("body",
    { target, _ -> listOf(target.body) },
    { target, _, bytes -> target }, { it }, { it })

/**
 * Extension functions for various body types
 */

fun Body.string(description: String? = null) = Body.map { String(it.array()) }.required("body", description)

fun Body.form() = StringLensSpec<Request>("form", {
    message, _ ->
    if (CONTENT_TYPE(message) != APPLICATION_FORM_URLENCODED) throw Invalid(Meta("form", "body"))
    else listOf(message.body.string())
},
    { msg, _, formBody -> formBody.fold(msg, { memo, next -> memo.body(next.toByteBuffer()) }) }
).map { it.toParameters() }.required("body")
