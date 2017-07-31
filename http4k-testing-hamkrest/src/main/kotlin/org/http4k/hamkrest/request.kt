package org.http4k.hamkrest

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.lens.QueryLens

fun <T> hasQuery(lens: QueryLens<T>, matcher: Matcher<T>): Matcher<Request> = LensMatcher(has("Query '${lens.meta.name}'", { req: Request -> lens(req) }, matcher))

fun hasQuery(name: String, expected: String?): Matcher<Request> = has("Query '$name'", { req: Request -> req.query(name) }, equalTo(expected))

fun hasQuery(name: String, expected: List<String?>): Matcher<Request> = has("Queries '$name'", { req: Request -> req.queries(name) }, equalTo(expected))

fun hasForm(name: String, expected: String?): Matcher<Request> = has("Form '$name'", { req: Request -> req.form(name) }, equalTo(expected))

fun hasMethod(expected: Method): Matcher<Request> = has("Method", { req: Request -> req.method }, equalTo(expected))

fun hasUri(expected: Uri): Matcher<Request> = hasUri(equalTo(expected))

fun hasUri(expected: String): Matcher<Request> = hasUri(has(Uri::toString, equalTo(expected)))

fun hasUri(expected: Matcher<Uri>): Matcher<Request> = has("Uri", { req: Request -> req.uri }, expected)

fun hasCookie(expected: Cookie): Matcher<Request> = hasCookie(expected.name, equalTo(expected))

fun hasCookie(name: String, expected: String): Matcher<Request> = hasCookie(name, hasCookieValue(expected))

fun hasCookie(name: String, expected: Matcher<Cookie>): Matcher<Request> = has("Cookie '$name'", { r: Request -> r.cookie(name)!! }, expected)
