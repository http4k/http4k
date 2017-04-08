package org.reekwest.http.core.contract

import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.toParameters
import java.nio.ByteBuffer

object Body : MessagePart<HttpMessage, ByteBuffer> {
    override fun get(msg: HttpMessage): org.reekwest.http.core.body.Body = msg.body!!
}

fun Body.string() = Body.map { String(it.array()) }

fun Body.form() = Body.string().map { it.toParameters() }