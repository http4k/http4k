package org.reekwest.http.core.body

import org.reekwest.http.core.Body
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import java.nio.ByteBuffer

fun Response.bodyString(body: String): Response = copy(body = body.toBody())

fun Request.bodyString(body: String): Request = copy(body = body.toBody())

fun HttpMessage.bodyString(): String = body.string()

fun String.toBody(): Body = ByteBuffer.wrap(toByteArray())

private fun Body?.string(): String = this?.let { String(it.array()) } ?: ""
