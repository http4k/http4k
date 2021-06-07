package org.http4k.hamkrest

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.present
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.SameSite
import java.time.LocalDateTime

fun hasCookieName(expected: CharSequence): Matcher<Cookie> = has(Cookie::name, equalTo(expected))

@JvmName("hasCookieValueNullableString")
fun hasCookieValue(matcher: Matcher<String?>): Matcher<Cookie> = has(Cookie::value, matcher)

fun hasCookieValue(matcher: Matcher<String>): Matcher<Cookie> = has(Cookie::value, present(matcher))

fun hasCookieValue(expected: CharSequence): Matcher<Cookie> = has(Cookie::value, equalTo(expected))

fun hasCookieDomain(expected: CharSequence): Matcher<Cookie> = has("domain", { c: Cookie -> c.domain }, equalTo(expected))

fun hasCookiePath(expected: CharSequence): Matcher<Cookie> = has("path", { c: Cookie -> c.path }, equalTo(expected))

fun isSecureCookie(expected: Boolean = true): Matcher<Cookie> = has("secure", { c: Cookie -> c.secure }, equalTo(expected))

fun isHttpOnlyCookie(expected: Boolean = true): Matcher<Cookie> = has("httpOnly", { c: Cookie -> c.httpOnly }, equalTo(expected))

fun hasCookieExpiry(expected: LocalDateTime): Matcher<Cookie> = hasCookieExpiry(equalTo(expected))

fun hasCookieExpiry(matcher: Matcher<LocalDateTime>): Matcher<Cookie> = has("expiry", { c: Cookie -> c.expires!! }, matcher)

fun hasCookieSameSite(expected: SameSite): Matcher<Cookie> = has("sameSite", { c: Cookie -> c.sameSite }, equalTo(expected))
