package org.reekwest.http.core.cookie

import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.cookie.CookieAttribute.Companion.COMMENT
import org.reekwest.http.core.cookie.CookieAttribute.Companion.DOMAIN
import org.reekwest.http.core.cookie.CookieAttribute.Companion.EXPIRES
import org.reekwest.http.core.cookie.CookieAttribute.Companion.HTTP_ONLY
import org.reekwest.http.core.cookie.CookieAttribute.Companion.MAX_AGE
import org.reekwest.http.core.cookie.CookieAttribute.Companion.PATH
import org.reekwest.http.core.cookie.CookieAttribute.Companion.SECURE
import org.reekwest.http.core.header
import org.reekwest.http.core.replaceHeader
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class CookieAttribute(val name: String) {
    companion object {
        val COMMENT = CookieAttribute("Comment")
        val DOMAIN = CookieAttribute("Domain")
        val MAX_AGE = CookieAttribute("Max-Age")
        val PATH = CookieAttribute("Path")
        val SECURE = CookieAttribute("Secure")
        val HTTP_ONLY = CookieAttribute("HttpOnly")
        val EXPIRES = CookieAttribute("Expires")
    }
}

fun Cookie.comment(comment: String) = attribute(COMMENT, comment)

fun Cookie.domain(domain: String) = attribute(DOMAIN, domain)

fun Cookie.maxAge(seconds: Int) = attribute(MAX_AGE, "$seconds")

fun Cookie.path(path: String) = attribute(PATH, path)

fun Cookie.secure() = attribute(SECURE, "")

fun Cookie.httpOnly() = attribute(HTTP_ONLY, "")

fun Cookie.expires(date: LocalDateTime): Cookie = attribute(EXPIRES, ZonedDateTime.of(date, ZoneId.of("GMT")).format(RFC822))

private fun Cookie.attribute(name: String, value: String): Cookie = copy(attributes = attributes.plus(name to value))

private fun Cookie.attribute(attribute: CookieAttribute, value: String): Cookie = this.attribute(attribute.name, value)

private val RFC822 = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz")

fun Response.cookie(cookie: Cookie): Response = header("Set-Cookie", cookie.toString())

fun Response.removeCookie(name: String): Response = copy(headers = headers.filterNot { it.first == "Set-Cookie" && (it.second?.startsWith("$name=") ?: false) })

fun Response.replaceCookie(cookie: Cookie): Response = removeCookie(cookie.name).cookie(cookie)

fun Request.cookie(name: String, value: String): Request = replaceHeader("Cookie", cookies().plus(Cookie(name, value)).toCookieString())

internal fun String.toCookieList(): List<Cookie> = split("; ").filter { it.trim().isNotBlank() }.map { it.split("=").let { Cookie(it.elementAt(0), it.elementAtOrElse(1, { "\"\"" }).unquoted()) } }

private fun Request.cookies(): List<Cookie> = headers.find { it.first == "Cookie" }?.second?.toCookieList() ?: listOf()

private fun String.unquoted(): String = replaceFirst("^\"".toRegex(), "").replaceFirst("\"$".toRegex(), "").replace("\\\"", "\"")

private fun List<Cookie>.toCookieString() = map(Cookie::toString).joinToString("")
