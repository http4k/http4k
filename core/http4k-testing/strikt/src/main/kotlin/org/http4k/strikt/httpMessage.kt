package org.http4k.strikt

import org.http4k.core.HttpMessage
import org.http4k.format.Json
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.HeaderLens
import strikt.api.Assertion

fun <T, M : HttpMessage> Assertion.Builder<M>.header(lens: HeaderLens<T>) = get { lens(this) }
fun <M : HttpMessage> Assertion.Builder<M>.header(name: String) = get { header(name) }
fun <M : HttpMessage> Assertion.Builder<M>.headerValues(name: String) = get { headerValues(name) }

val <M : HttpMessage> Assertion.Builder<M>.contentType get() = get { CONTENT_TYPE(this) }
val <M : HttpMessage> Assertion.Builder<M>.body get() = get(HttpMessage::body)
val <M : HttpMessage> Assertion.Builder<M>.bodyString get() = get { bodyString() }

fun <NODE, M : HttpMessage> Assertion.Builder<M>.jsonBody(json: Json<NODE>) = get { json.parse(bodyString()) }
