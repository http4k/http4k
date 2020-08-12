package org.http4k.kotest

import io.kotest.matchers.Matcher
import io.kotest.matchers.be
import io.kotest.matchers.neverNullMatcher
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import io.kotest.matchers.string.contain
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.lens.QueryLens

fun <T> Request.shouldHaveQuery(lens: QueryLens<T>, matcher: Matcher<T>) = this should haveQuery(lens, matcher)
fun <T> Request.shouldNotHaveQuery(lens: QueryLens<T>, matcher: Matcher<T>) = this shouldNot haveQuery(lens, matcher)
fun <T> haveQuery(lens: QueryLens<T>, matcher: Matcher<T>): Matcher<Request> = LensMatcher(httpMessageHas("Query \"${lens.meta.name}\"", { req: Request -> lens(req) }, matcher))

@JvmName("shouldHaveQueryNullableStringMatcher")
fun Request.shouldHaveQuery(name: String, matcher: Matcher<String?>) = this should haveQuery(name, matcher)

@JvmName("shouldNotHaveQueryNullableStringMatcher")
fun Request.shouldNotHaveQuery(name: String, matcher: Matcher<String?>) = this shouldNot haveQuery(name, matcher)

@JvmName("haveQueryNullableStringMatcher")
fun haveQuery(name: String, matcher: Matcher<String?>): Matcher<Request> = httpMessageHas("Query \"$name\"", { req: Request -> req.query(name) }, matcher)

fun Request.shouldHaveQuery(name: String, matcher: Matcher<String>) = this should haveQuery(name, matcher)
fun Request.shouldNotHaveQuery(name: String, matcher: Matcher<String>) = this shouldNot haveQuery(name, matcher)
fun haveQuery(name: String, matcher: Matcher<String>): Matcher<Request> = httpMessageHas("Query \"$name\"", { req: Request -> req.query(name) }, neverNullMatcher(matcher::test))

fun Request.shouldHaveQuery(name: String, expected: CharSequence) = this should haveQuery(name, expected)
fun Request.shouldNotHaveQuery(name: String, expected: CharSequence) = this shouldNot haveQuery(name, expected)
fun haveQuery(name: String, expected: CharSequence): Matcher<Request> = haveQuery(name, be(expected.toString()))

fun Request.shouldHaveQuery(name: String, expected: Regex) = this should haveQuery(name, expected)
fun Request.shouldNotHaveQuery(name: String, expected: Regex) = this shouldNot haveQuery(name, expected)
fun haveQuery(name: String, expected: Regex): Matcher<Request> = haveQuery(name, contain(expected))

fun Request.shouldHaveQuery(name: String, expected: List<String?>) = this should haveQuery(name, expected)
fun Request.shouldNotHaveQuery(name: String, expected: List<String?>) = this shouldNot haveQuery(name, expected)
fun haveQuery(name: String, expected: List<String?>): Matcher<Request> = httpMessageHas("Query \"$name\"", { req: Request -> req.queries(name) }, be(expected))

@JvmName("shouldHaveFormNullableStringMatcher")
fun Request.shouldHaveForm(name: String, matcher: Matcher<String?>) = this should haveForm(name, matcher)

@JvmName("shouldNotHaveFormNullableStringMatcher")
fun Request.shouldNotHaveForm(name: String, matcher: Matcher<String?>) = this shouldNot haveForm(name, matcher)

@JvmName("haveFormNullableStringMatcher")
fun haveForm(name: String, matcher: Matcher<String?>): Matcher<Request> = httpMessageHas("Form \"$name\"", { req: Request -> req.form(name) }, matcher)

fun Request.shouldHaveForm(name: String, matcher: Matcher<String>) = this should haveForm(name, matcher)
fun Request.shouldNotHaveForm(name: String, matcher: Matcher<String>) = this shouldNot haveForm(name, matcher)
fun haveForm(name: String, matcher: Matcher<String>): Matcher<Request> = httpMessageHas("Form \"$name\"", { req: Request -> req.form(name) }, neverNullMatcher(matcher::test))

fun Request.shouldHaveForm(name: String, matcher: Regex) = this should haveForm(name, matcher)
fun Request.shouldNotHaveForm(name: String, matcher: Regex) = this shouldNot haveForm(name, matcher)
fun haveForm(name: String, expected: Regex): Matcher<Request> = haveForm(name, contain(expected))

fun Request.shouldHaveForm(name: String, expected: CharSequence) = this should haveForm(name, expected)
fun Request.shouldNotHaveForm(name: String, expected: CharSequence) = this shouldNot haveForm(name, expected)
fun haveForm(name: String, expected: CharSequence): Matcher<Request> = haveForm(name, be(expected.toString()))

infix fun Request.shouldHaveMethod(expected: Method) = this should haveMethod(expected)
infix fun Request.shouldNotHaveMethod(expected: Method) = this shouldNot haveMethod(expected)
fun haveMethod(expected: Method): Matcher<Request> = httpMessageHas("Method", { req: Request -> req.method }, be(expected))

infix fun Request.shouldHaveUri(expected: Regex) = this should haveUri(expected)
infix fun Request.shouldNotHaveUri(expected: Regex) = this shouldNot haveUri(expected)
fun haveUri(expected: Regex): Matcher<Request> = haveUri(contain(expected).compose(Uri::toString))

infix fun Request.shouldHaveUri(expected: Uri) = this should haveUri(expected)
infix fun Request.shouldNotHaveUri(expected: Uri) = this shouldNot haveUri(expected)
fun haveUri(expected: Uri): Matcher<Request> = haveUri(be(expected))

infix fun Request.shouldHaveUri(expected: String) = this should haveUri(expected)
infix fun Request.shouldNotHaveUri(expected: String) = this shouldNot haveUri(expected)
fun haveUri(expected: String): Matcher<Request> = haveUri(be(expected).compose(Uri::toString))

infix fun Request.shouldHaveUri(match: Matcher<Uri>) = this should haveUri(match)
infix fun Request.shouldNotHaveUri(match: Matcher<Uri>) = this shouldNot haveUri(match)
fun haveUri(expected: Matcher<Uri>): Matcher<Request> = httpMessageHas("Uri", { req: Request -> req.uri }, expected)

infix fun Request.shouldHaveCookie(expected: Cookie) = this should haveCookie(expected)
infix fun Request.shouldNotHaveCookie(expected: Cookie) = this shouldNot haveCookie(expected)
fun haveCookie(expected: Cookie): Matcher<Request> = haveCookie(expected.name, be(expected))

fun Request.shouldHaveCookie(name: String, expected: Regex) = this should haveCookie(name, expected)
fun Request.shouldNotHaveCookie(name: String, expected: Regex) = this shouldNot haveCookie(name, expected)
fun haveCookie(name: String, expected: Regex): Matcher<Request> = haveCookie(name, haveValue(contain(expected)))

fun Request.shouldHaveCookie(name: String, expected: String) = this should haveCookie(name, expected)
fun Request.shouldNotHaveCookie(name: String, expected: String) = this shouldNot haveCookie(name, expected)
fun haveCookie(name: String, expected: String): Matcher<Request> = haveCookie(name, haveValue(expected))

fun Request.shouldHaveCookie(name: String, match: Matcher<Cookie>) = this should haveCookie(name, match)
fun Request.shouldNotHaveCookie(name: String, match: Matcher<Cookie>) = this shouldNot haveCookie(name, match)
fun haveCookie(name: String, match: Matcher<Cookie>): Matcher<Request> = httpMessageHas("Cookie \"$name\"", { r: Request -> r.cookie(name)!! }, match)
