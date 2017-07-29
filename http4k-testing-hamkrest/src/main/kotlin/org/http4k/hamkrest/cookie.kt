package org.http4k.hamkrest

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import org.http4k.core.cookie.Cookie
import java.time.LocalDateTime

fun cookieName(expected: String) = object : Matcher<Cookie> {
    override val description = "Cookie with Name of $expected"
    override fun invoke(actual: Cookie) = equalTo(expected)(actual.name)
}

fun cookieValue(expected: String) = object : Matcher<Cookie> {
    override val description = "Cookie with Value of $expected"
    override fun invoke(actual: Cookie) = equalTo(expected)(actual.value)
}

fun cookieDomain(expected: String) = object : Matcher<Cookie> {
    override val description = "Cookie with Domain of $expected"
    override fun invoke(actual: Cookie) = equalTo(expected)(actual.domain)
}

fun cookiePath(expected: String) = object : Matcher<Cookie> {
    override val description = "Cookie with Path of $expected"
    override fun invoke(actual: Cookie) = equalTo(expected)(actual.path)
}

fun cookieIsSecure(expected: Boolean = true) = object : Matcher<Cookie> {
    override val description = "Cookie with Secure Flag of $expected"
    override fun invoke(actual: Cookie) = equalTo(expected)(actual.secure)
}

fun cookieIsHttpOnly(expected: Boolean = true) = object : Matcher<Cookie> {
    override val description = "Cookie with HttpOnly Flag of $expected"
    override fun invoke(actual: Cookie) = equalTo(expected)(actual.httpOnly)
}

fun cookieExpiry(expected: LocalDateTime) = cookieExpiry(equalTo(expected))

fun cookieExpiry(expected: Matcher<LocalDateTime?>) = object : Matcher<Cookie> {
    override val description = "Cookie with Expiry of $expected"
    override fun invoke(actual: Cookie) = expected(actual.expires)
}
