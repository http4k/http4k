package org.reekwest.http.core.contract

import org.reekwest.http.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Request
import org.reekwest.http.core.body.Form
import org.reekwest.http.core.toParameters
import java.nio.ByteBuffer

object Body : MessagePart<HttpMessage, ByteBuffer> {
    override fun get(msg: HttpMessage): org.reekwest.http.core.body.Body = msg.body!!
}

/**
 * Extension functions for various body types
 */

fun Body.string() = Body.map { String(it.array()) }

fun Body.form() = object : MessagePart<Request, Form> {
    private val contentType = Header.required("Content-Type")
    override fun toString(): String = "form body"

    override fun get(msg: Request): Form =
        if (msg[contentType] != APPLICATION_FORM_URLENCODED.value) throw Invalid(this)
        else String(msg.body!!.array()).toParameters()
}

