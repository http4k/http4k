package org.http4k.http.core.body

import org.http4k.http.core.Body
import org.http4k.http.core.Parameters
import org.http4k.http.core.Request
import org.http4k.http.core.findSingle
import org.http4k.http.core.toParameters
import org.http4k.http.core.toUrlEncoded
import java.nio.ByteBuffer

typealias Form = Parameters

fun Request.form(name: String): String? = form().findSingle(name)

fun Form.toBody(): Body = ByteBuffer.wrap(toUrlEncoded().toByteArray())

fun Request.form(): Form = body?.let { String(it.array()) }?.toParameters() ?: emptyList()
