package org.http4k.core.body

import org.http4k.core.Body
import org.http4k.core.Parameters
import org.http4k.core.Request
import org.http4k.core.findSingle
import org.http4k.core.toParameters
import org.http4k.core.toUrlEncoded

typealias Form = Parameters

fun Request.form(name: String): String? = form().findSingle(name)

fun Form.toBody(): Body = Body(toUrlEncoded())

fun Request.form(): Form = body.toString().toParameters()
