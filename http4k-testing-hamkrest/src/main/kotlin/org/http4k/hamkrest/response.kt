package org.http4k.hamkrest

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookies

fun hasStatus(expected: Status) = has(Response::status, equalTo(expected))

fun hasSetCookie(expected: Cookie): Matcher<Response> = hasSetCookie(expected.name, equalTo(expected))

fun hasSetCookie(name: String, expected: Matcher<Cookie>): Matcher<Response> = has("Cookie '$name'", { r: Response -> r.cookies().find { name == it.name }!! }, expected)
