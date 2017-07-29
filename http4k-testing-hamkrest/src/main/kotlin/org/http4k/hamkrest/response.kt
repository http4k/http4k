package org.http4k.hamkrest

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookies

fun status(expected: Status) = object : Matcher<Response> {
    override val description = "Response with Status of $expected"
    override fun invoke(actual: Response) = equalTo(expected)(actual.status)
}

fun setCookie(expected: Cookie) = object : Matcher<Response> {
    override val description = "Response with Cookie $expected"
    override fun invoke(actual: Response) = equalTo(expected)(actual.cookieWith(expected.name))
}

fun setCookie(name: String, expected: String) = object : Matcher<Response> {
    override val description = "Response with Cookie $name=$expected"
    override fun invoke(actual: Response) = equalTo(expected)(actual.cookieWith(name)?.value)
}

fun setCookie(name: String, expected: Matcher<Cookie?>) = object : Matcher<Response> {
    override val description = "Response with Cookie $name"
    override fun invoke(actual: Response) = expected(actual.cookieWith(name))
}

private fun Response.cookieWith(name: String) = cookies().find { name == it.name }
