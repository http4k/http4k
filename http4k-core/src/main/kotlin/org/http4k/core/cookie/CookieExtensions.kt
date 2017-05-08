package org.http4k.core.cookie

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.unquoted
import java.time.LocalDateTime
import java.time.ZoneOffset

fun Response.cookie(cookie: Cookie): Response = header("Set-Cookie", cookie.toString())

fun Response.removeCookie(name: String): Response {
    val oldCookies = headerValues("Set-Cookie")
    val next = removeHeader("Set-Cookie")
    return oldCookies.filter { (it != null && !it.startsWith("$name=")) }.fold(next, { response, value -> response.header("Set-Cookie", value)})
}

fun Response.replaceCookie(cookie: Cookie): Response = removeCookie(cookie.name).cookie(cookie)

fun Request.cookie(name: String, value: String): Request = replaceHeader("Cookie", cookies().plus(Cookie(name, value)).toCookieString())

internal fun String.toCookieList(): List<Cookie> = split("; ").filter { it.trim().isNotBlank() }.map { it.split("=").let { Cookie(it.elementAt(0), it.elementAtOrElse(1, { "\"\"" }).unquoted()) } }

fun Request.cookies(): List<Cookie> = header("Cookie")?.toCookieList() ?: listOf()

fun Request.cookie(name: String): Cookie? = cookies().filter { it.name == name }.sortedByDescending { it.path?.length ?: 0 }.firstOrNull()

private fun List<Cookie>.toCookieString() = map(Cookie::toString).joinToString("")

fun Response.cookies(): List<Cookie> = headerValues("set-cookie").filterNotNull().map { Cookie.parse(it) }.filterNotNull()

fun Cookie.invalidate(): Cookie = copy(value = "").maxAge(0).expires(LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC))

fun Response.invalidateCookie(name: String): Response = replaceCookie(Cookie(name, "").invalidate())