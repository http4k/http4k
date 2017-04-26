package org.reekwest.http.core.body

import org.reekwest.http.core.Body
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Parameters
import org.reekwest.http.core.Request
import org.reekwest.http.core.findSingle
import org.reekwest.http.core.toParameters
import org.reekwest.http.core.toUrlEncoded
import java.nio.ByteBuffer

typealias Form = Parameters

fun Request.form(name: String): String? = form().findSingle(name)

fun Form.toBody(): Body = ByteBuffer.wrap(toUrlEncoded().toByteArray())

fun HttpMessage.form(): Form = body?.let { String(it.array()) }?.toParameters() ?: emptyList()
