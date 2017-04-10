package org.reekwest.http.core.contract

import org.reekwest.http.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.contract.Header.Common.CONTENT_TYPE
import org.reekwest.http.core.toParameters
import java.nio.ByteBuffer

object Body : LensSpec<HttpMessage, ByteBuffer>("body", { message, _ -> listOf(message.body) })

/**
 * Extension functions for various body types
 */

fun Body.string(description: String? = null) = Body.map { String(it.array()) }.required("body", description)

fun Body.form() = LensSpec<HttpMessage, ByteBuffer>("form", {
    message, _ ->
    if (CONTENT_TYPE(message) != APPLICATION_FORM_URLENCODED) throw Invalid(Meta("form", "body")) else listOf(message.body!!)
}).map { String(it.array()).toParameters() }.required("body")
