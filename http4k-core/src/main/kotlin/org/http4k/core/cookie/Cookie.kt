package org.http4k.core.cookie

import org.http4k.core.Parameters
import org.http4k.quoted
import org.http4k.unquoted
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ofPattern
import java.util.*
import java.util.Locale.US

data class Cookie(val name: String, val value: String,
                  val maxAge: Long? = null,
                  val expires: LocalDateTime? = null,
                  val domain: String? = null,
                  val path: String? = null,
                  val secure: Boolean = false,
                  val httpOnly: Boolean = false,
                  val sameSite: SameSite? = null) {

    fun domain(domain: String) = copy(domain = domain)
    fun maxAge(seconds: Long) = copy(maxAge = seconds)
    fun path(path: String) = copy(path = path)
    fun secure() = copy(secure = true)
    fun httpOnly() = copy(httpOnly = true)
    fun expires(date: LocalDateTime): Cookie = copy(expires = date)
    fun sameSite(sameSite: SameSite) = copy(sameSite = sameSite)

    override fun toString(): String = fullCookieString()

    private fun attributes(): String = mutableListOf<String>().apply {
        appendIfPresent(maxAge, "Max-Age=$maxAge")
        appendIfPresent(expires, "Expires=${expires?.let { ZonedDateTime.of(it, ZoneId.of("GMT")).format(RFC822) }}")
        appendIfPresent(domain, "Domain=$domain")
        appendIfPresent(path, "Path=$path")
        appendIfTrue(secure, "secure")
        appendIfTrue(httpOnly, "HttpOnly")
        appendIfPresent(sameSite, "SameSite=$sameSite")
    }.joinToString("; ")

    companion object {
        fun parse(cookieValue: String): Cookie? {
            val pair = cookieValue.split("=", limit = 2)
            return when {
                pair.size < 2 -> null
                else -> {
                    val valueAndAttributes = pair[1].split(";")
                    val attributes = valueAndAttributes.drop(1)
                        .map { it.trimStart() }
                        .map { attrString -> attrString.split("=").let { it[0] to it.getOrNull(1) } }
                    Cookie(pair[0], valueAndAttributes[0].unquoted(),
                        attributes.maxAge(), attributes.expires(), attributes.domain(),
                        attributes.path(), attributes.secure(), attributes.httpOnly(), attributes.sameSite())
                }
            }
        }

        private fun Parameters.maxAge(): Long? = find { it.first.equals("Max-Age", true) }?.second?.toLong()
        private fun Parameters.expires(): LocalDateTime? = find { it.first.equals("Expires", true) }?.second?.parseDate()
        private fun Parameters.domain(): String? = find { it.first.equals("Domain", true) }?.second
        private fun Parameters.path(): String? = find { it.first.equals("Path", true) }?.second
        private fun Parameters.secure(): Boolean = find { it.first.equals("secure", true) } != null
        private fun Parameters.httpOnly(): Boolean = find { it.first.equals("HttpOnly", true) } != null
        private fun Parameters.sameSite(): SameSite? = find { it.first.equals("SameSite", true) }?.second?.parseSameSite()

        private fun String.parseDate(): LocalDateTime? {
            println("got $this")
            for (supportedFormat in supportedFormats) {
                try {
                    return supportedFormat.parse(this).let { LocalDateTime.from(it) }
                } catch (e: Exception) {
                    println("error: $e")
                }
            }
            return null
        }

        private fun String.parseSameSite() = try {
            SameSite.valueOf(this)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun MutableList<String>.appendIfPresent(toCheck: Any?, toInclude: String) {
        if (toCheck != null) add(toInclude)
    }

    private fun MutableList<String>.appendIfTrue(toCheck: Boolean, toInclude: String) {
        if (toCheck) add(toInclude)
    }

    fun fullCookieString(): String = "$name=${value.quoted()}; ${attributes()}"
    fun keyValueCookieString(): String = "$name=${value.quoted()}"
}

enum class SameSite {
    Strict, Lax, None
}

private val RFC822 = ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", US)

private val supportedFormats = listOf(RFC822,
    ofPattern("EEE, dd-MMM-yyyy HH:mm:ss zzz", US),
    ofPattern("EEE, dd-MMM-yy HH:mm:ss zzz", US),
    ofPattern("EEE, dd MMM yy HH:mm:ss zzz", US),
    ofPattern("EEE MMM dd yy HH:mm:ss zzz", US),
    ofPattern("EEE MMM dd yyyy HH:mm:ss zzz", US)
)
