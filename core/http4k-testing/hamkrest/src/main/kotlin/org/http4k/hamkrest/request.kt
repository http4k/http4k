package org.http4k.hamkrest

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.matches
import com.natpryce.hamkrest.present
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.lens.QueryLens

fun <T> hasQuery(lens: QueryLens<T>, matcher: Matcher<T>): Matcher<Request> = LensMatcher(httpMessageHas("Query '${lens.meta.name}'", { req: Request -> lens(req) }, matcher))

@JvmName("hasQueryNullableString")
fun hasQuery(name: String, matcher: Matcher<String?>): Matcher<Request> = httpMessageHas("Query '$name'", { req: Request -> req.query(name) }, matcher)

fun hasQuery(name: String, matcher: Matcher<String>): Matcher<Request> = httpMessageHas("Query '$name'", { req: Request -> req.query(name) }, present(matcher))

fun hasQuery(name: String, expected: CharSequence): Matcher<Request> = hasQuery(name, equalTo(expected))

fun hasQuery(name: String, expected: Regex): Matcher<Request> = hasQuery(name, present(matches(expected)))

fun hasQuery(name: String, expected: List<String?>): Matcher<Request> = httpMessageHas("Queries '$name'", { req: Request -> req.queries(name) }, equalTo(expected))

@JvmName("hasFormNullableString")
fun hasForm(name: String, matcher: Matcher<String?>): Matcher<Request> = httpMessageHas("Form '$name'", { req: Request -> req.form(name) }, present(matcher))

fun hasForm(name: String, matcher: Matcher<String>): Matcher<Request> = httpMessageHas("Form '$name'", { req: Request -> req.form(name) }, present(matcher))

fun hasForm(name: String, expected: Regex): Matcher<Request> = hasForm(name, matches(expected))

fun hasForm(name: String, expected: CharSequence): Matcher<Request> = hasForm(name, equalTo(expected))

fun hasMethod(expected: Method): Matcher<Request> = httpMessageHas("Method", { req: Request -> req.method }, equalTo(expected))

fun hasUri(expected: Regex): Matcher<Request> = hasUri(has(Uri::toString, matches(expected)))

fun hasUri(expected: Uri): Matcher<Request> = hasUri(equalTo(expected))

fun hasUri(expected: String): Matcher<Request> = hasUri(has(Uri::toString, equalTo(expected)))

fun hasUri(expected: Matcher<Uri>): Matcher<Request> = httpMessageHas("Uri", { req: Request -> req.uri }, expected)

fun hasCookie(expected: Cookie): Matcher<Request> = hasCookie(expected.name, equalTo(expected))

fun hasCookie(name: String, expected: Regex): Matcher<Request> = hasCookie(name, hasCookieValue(matches(expected)))

fun hasCookie(name: String, expected: String): Matcher<Request> = hasCookie(name, hasCookieValue(expected))

fun hasCookie(name: String, matcher: Matcher<Cookie>): Matcher<Request> = httpMessageHas("Cookie '$name'", { r: Request -> r.cookie(name)!! }, matcher)
