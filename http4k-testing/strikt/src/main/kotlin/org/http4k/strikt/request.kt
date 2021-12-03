package org.http4k.strikt


import org.http4k.core.Request
import org.http4k.core.body.form
import org.http4k.core.cookie.cookie
import org.http4k.lens.Lens
import org.http4k.lens.QueryLens
import org.http4k.lens.WebForm
import strikt.api.Assertion

val Assertion.Builder<Request>.uri get() = get(Request::uri)
val Assertion.Builder<Request>.method get() = get(Request::method)

fun <T> Assertion.Builder<Request>.query(lens: QueryLens<T>) = get { lens(this) }
fun Assertion.Builder<Request>.query(name: String) = get { query(name) }
fun Assertion.Builder<Request>.queries(name: String) = get { queries(name) }

fun Assertion.Builder<Request>.form(name: String) = get { form(name) }

fun Assertion.Builder<Request>.cookie(name: String) = get { cookie(name) }

fun <T> Assertion.Builder<WebForm>.field2(lens: Lens<WebForm, T>) = get { lens(this) }
