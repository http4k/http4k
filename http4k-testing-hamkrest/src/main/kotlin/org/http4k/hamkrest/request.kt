package org.http4k.hamkrest

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie

fun hasQuery(name: String, expected: String?): Matcher<Request> = has("Query", { req: Request -> req.query(name) }, equalTo(expected))

fun hasQuery(name: String, expected: List<String?>): Matcher<Request> = has("Queries", { req: Request -> req.queries(name) }, equalTo(expected))

fun hasMethod(expected: Method): Matcher<Request> = has("Method", { req: Request -> req.method }, equalTo(expected))

fun hasUri(expected: Uri): Matcher<Request> = hasUri(equalTo(expected))

fun hasUri(expected: String): Matcher<Request> = hasUri(has(Uri::toString, equalTo(expected)))

fun hasUri(expected: Matcher<Uri>): Matcher<Request> = has("uri", { req: Request -> req.uri }, expected)

fun hasCookie(expected: Cookie): Matcher<Request> = hasCookie(expected.name, equalTo(expected))

fun hasCookie(name: String, expected: String): Matcher<Request> = hasCookie(name, hasCookieValue(expected))

fun hasCookie(name: String, expected: Matcher<Cookie>): Matcher<Request> = has("cookie", { r: Request -> r.cookie(name)!! }, expected)
