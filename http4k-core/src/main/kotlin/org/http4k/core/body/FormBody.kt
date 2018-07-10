package org.http4k.core.body

import org.http4k.core.*

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

fun Request.form(name:String, value:String): Request = body(form().plus(name to value).toBody())
