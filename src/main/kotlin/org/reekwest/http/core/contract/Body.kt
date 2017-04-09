package org.reekwest.http.core.contract

import org.reekwest.http.core.ContentType
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Request
import org.reekwest.http.core.body.Form
import org.reekwest.http.core.toParameters
import java.nio.ByteBuffer

object Body : Required<HttpMessage, ByteBuffer>({ it.body!! })

/**
 * Extension functions for various body types
 */

fun Body.string() = Body.map { String(it.array()) }

fun Body.form() = Required<Request, Form>({
    val contentType = Header.required("Content-Type")
    if (it[contentType] != ContentType.APPLICATION_FORM_URLENCODED.value) throw Invalid(this)
    else String(it.body!!.array()).toParameters()
})
