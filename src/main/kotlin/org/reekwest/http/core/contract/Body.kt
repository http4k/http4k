package org.reekwest.http.core.contract

import org.reekwest.http.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Request
import org.reekwest.http.core.body.Form
import org.reekwest.http.core.contract.Header.Common.CONTENT_TYPE
import org.reekwest.http.core.toParameters
import java.nio.ByteBuffer

object Body : Required<HttpMessage, ByteBuffer>(Meta("body", "body"), { it.body!! })

/**
 * Extension functions for various body types
 */

fun Body.string(description: String? = null)
    = Required<HttpMessage, String>(Meta("body", "body", description), { String(it.body!!.array()) })

fun Body.form() = Required<Request, Form>(Meta("form", "body"), {
    if (it[CONTENT_TYPE] != APPLICATION_FORM_URLENCODED) throw Invalid(this)
    else String(it.body!!.array()).toParameters()
})
