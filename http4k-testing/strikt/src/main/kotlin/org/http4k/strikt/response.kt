package org.http4k.strikt

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.cookie.cookie
import strikt.api.Assertion

val Assertion.Builder<Response>.status get() = get(Response::status)
fun Assertion.Builder<Request>.setCookie(name: String) = get { cookie(name) }
