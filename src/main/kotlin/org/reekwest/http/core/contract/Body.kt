package org.reekwest.http.core.contract

import org.reekwest.http.core.HttpMessage
import java.nio.ByteBuffer

object Body : Spec<HttpMessage, ByteBuffer>("body", { message, _ -> listOf(message.body)})

/**
 * Extension functions for various body types
 */

fun Body.string(description: String? = null) = Body.map { String(it.array()) }.required("body", description)

//
//fun Body.form() = Required<Request, Form>(Meta("form", "body"), {
//    if (it[CONTENT_TYPE] != APPLICATION_FORM_URLENCODED) throw Invalid(this)
//    else String(it.body!!.array()).toParameters()
//})
