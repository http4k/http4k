package org.http4k.core.body

import org.http4k.core.Body
import org.http4k.core.Parameters
import org.http4k.core.Request
import org.http4k.core.findSingle
import org.http4k.core.toParameters
import org.http4k.core.toParametersMap
import org.http4k.core.toUrlFormEncoded

typealias Form = Parameters

/**
 * Returns the first form parameter with [name].
 *
 * Use [formAsMap] if you don't want to decode the body every invocation.
 */
fun Request.form(name: String): String? = form().findSingle(name)

fun Request.formAsMap(): Map<String, List<String?>> = form().toParametersMap()

fun Form.toBody(): Body = Body(toUrlFormEncoded())

fun Request.form(): Form = bodyString().toParameters()

fun Request.form(name: String, value: String): Request = body(form().plus(name to value).toBody())
