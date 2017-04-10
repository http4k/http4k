package org.reekwest.http.core.body

import org.reekwest.http.core.Body
import org.reekwest.http.core.Response
import java.nio.ByteBuffer

fun Body?.string() = StringBody.from(this)

fun Response.bodyString(body: String): Response = copy(body = body.toBody())

fun String.toBody() = StringBody.to(this)

object StringBody : BodyRepresentation<String> {
    override fun from(body: Body?): String = body?.let { String(it.array()) } ?: ""
    override fun to(value: String): Body = ByteBuffer.wrap(value.toByteArray())
}
