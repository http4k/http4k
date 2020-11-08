package org.http4k.kotest

import io.kotest.matchers.Matcher
import io.kotest.matchers.be
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookies

infix fun Response.shouldHaveStatus(expected: Status) = this should haveStatus(expected)
infix fun Response.shouldNotHaveStatus(expected: Status) = this shouldNot haveStatus(expected)
fun haveStatus(expected: Status): Matcher<Response> = httpMessageHas("Status", Response::status, be(expected))

infix fun Response.shouldHaveSetCookie(expected: Cookie) = this should haveSetCookie(expected)
infix fun Response.shouldNotHaveSetCookie(expected: Cookie) = this shouldNot haveSetCookie(expected)
fun haveSetCookie(expected: Cookie): Matcher<Response> = haveSetCookie(expected.name, be(expected))

fun Response.shouldHaveSetCookie(name: String, match: Matcher<Cookie>) = this should haveSetCookie(name, match)
fun Response.shouldNotHaveSetCookie(name: String, match: Matcher<Cookie>) = this shouldNot haveSetCookie(name, match)
fun haveSetCookie(name: String, expected: Matcher<Cookie>): Matcher<Response> = httpMessageHas("Cookie \"$name\"", { r: Response -> r.cookies().find { name == it.name }!! }, expected)

