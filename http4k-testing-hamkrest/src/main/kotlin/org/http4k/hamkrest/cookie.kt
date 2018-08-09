package org.http4k.hamkrest

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import org.http4k.core.cookie.Cookie
import java.time.LocalDateTime

fun hasCookieName(expected: CharSequence): Matcher<Cookie> = has(Cookie::name, equalTo(expected))

fun hasCookieValue(matcher: Matcher<CharSequence>): Matcher<Cookie> = has(Cookie::value, matcher)

fun hasCookieValue(expected: CharSequence): Matcher<Cookie> = has(Cookie::value, equalTo(expected))

fun hasCookieDomain(expected: CharSequence): Matcher<Cookie> = has("domain", { c: Cookie -> c.domain }, equalTo(expected))

fun hasCookiePath(expected: CharSequence): Matcher<Cookie> = has("path", { c: Cookie -> c.path }, equalTo(expected))

fun isSecureCookie(expected: Boolean = true): Matcher<Cookie> = has("secure", { c: Cookie -> c.secure }, equalTo(expected))

fun isHttpOnlyCookie(expected: Boolean = true): Matcher<Cookie> = has("httpOnly", { c: Cookie -> c.httpOnly }, equalTo(expected))

fun hasCookieExpiry(expected: LocalDateTime): Matcher<Cookie> = hasCookieExpiry(equalTo(expected))

fun hasCookieExpiry(matcher: Matcher<LocalDateTime>): Matcher<Cookie> = has("expiry", { c: Cookie -> c.expires!! }, matcher)
