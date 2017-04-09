package org.reekwest.http.core.contract

//object Body : Required<HttpMessage, ByteBuffer>(Meta("body", "body"), { it.body!! })
//
///**
// * Extension functions for various body types
// */
//
//fun Body.string(description: String? = null)
//    = Required<HttpMessage, String>(Meta("body", "body", description), { String(it.body!!.array()) })
//
//fun Body.form() = Required<Request, Form>(Meta("form", "body"), {
//    if (it[CONTENT_TYPE] != APPLICATION_FORM_URLENCODED) throw Invalid(this)
//    else String(it.body!!.array()).toParameters()
//})
