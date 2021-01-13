package org.http4k.core.cookie

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.unquoted
import java.time.LocalDateTime
import java.time.ZoneOffset

fun Response.cookie(cookie: Cookie): Response = header("Set-Cookie", cookie.fullCookieString())

fun Request.removeCookie(name: String) =
    cookies().filterNot { it.name == name }.fold(removeHeader("Cookie")) { acc, c -> acc.cookie(c) }

fun Response.removeCookie(name: String): Response {
    val oldCookies = headerValues("Set-Cookie")
    val next = removeHeader("Set-Cookie")
    return oldCookies.filter { (it != null && !it.startsWith("$name=")) }.fold(next) { acc, c -> acc.header("Set-Cookie", c) }
}

fun Response.replaceCookie(cookie: Cookie): Response = removeCookie(cookie.name).cookie(cookie)

fun Request.cookie(name: String, value: String): Request = replaceHeader("Cookie", cookies().plus(Cookie(name, value)).toCookieString())

fun Request.cookie(new: Cookie): Request = replaceHeader("Cookie", cookies().plus(new).toCookieString())

internal fun String.toCookieList(): List<Cookie> = split(";").map { it.trim() }.filter { it.isNotBlank() }.map { it.split("=", limit = 2).let { Cookie(it.elementAt(0), it.elementAtOrElse(1) { "\"\"" }.unquoted()) } }

fun Request.cookies(): List<Cookie> = headers
    .filter { it.first.equals("cookie", true) }
    .mapNotNull { it.second?.toCookieList() }
    .fold(listOf()) { acc, current -> acc.plus(current) }

fun Request.cookie(name: String): Cookie? = cookies().filter { it.name == name }.maxByOrNull {
    it.path?.length ?: 0
}

private fun List<Cookie>.toCookieString() = joinToString("; ", transform = Cookie::keyValueCookieString)

fun Response.cookies(): List<Cookie> = headerValues("set-cookie").filterNotNull().mapNotNull { Cookie.parse(it) }

fun Cookie.invalidate(): Cookie = copy(value = "").maxAge(0).expires(LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC))

fun Response.invalidateCookie(name: String, domain: String? = null): Response = replaceCookie(Cookie(name, "", domain = domain).invalidate())
