package org.reekwest.http.core.contract

//object Body : LensSpec<HttpMessage, ByteBuffer>("body",
//    { message, _ -> listOf(message.body) },
//    { msg, _, bytes -> TODO() })
//
///**
// * Extension functions for various body types
// */
//
//fun Body.string(description: String? = null) = Body.map { String(it.array()) }.required("body", description)
//
//fun Body.form() = LensSpec<HttpMessage, ByteBuffer>("form", {
//    message, _ ->
//    if (CONTENT_TYPE(message) != APPLICATION_FORM_URLENCODED) throw Invalid(Meta("form", "body")) else listOf(message.body!!)
//}, {msg, _, bytes -> msg}).map { String(it.array()).toParameters() }.required("body")
