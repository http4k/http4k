package org.http4k.hamkrest

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.has
import org.http4k.core.HttpTransaction
import org.http4k.core.Request
import org.http4k.core.Response

fun hasRequest(matcher: Matcher<Request>) = has("Request", { tx: HttpTransaction -> tx.request }, matcher)
fun hasResponse(matcher: Matcher<Response>) = has("Response", { tx: HttpTransaction -> tx.response }, matcher)
