package org.http4k.http.core.cookie

import org.http4k.http.quoted
import org.http4k.http.unquoted
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class Cookie(val name: String, val value: String,
                  val maxAge: Long? = null,
                  val expires: LocalDateTime? = null,
                  val domain: String? = null,
                  val path: String? = null,
                  val secure: Boolean = false,
                  val httpOnly: Boolean = false) {

    fun domain(domain: String) = copy(domain = domain)
    fun maxAge(seconds: Long) = copy(maxAge = seconds)
    fun path(path: String) = copy(path = path)
    fun secure() = copy(secure = true)
    fun httpOnly() = copy(httpOnly = true)
    fun expires(date: LocalDateTime): Cookie = copy(expires = date)

    override fun toString(): String = "$name=${value.quoted()}; ${attributes()}"

    private fun attributes(): String {
        val builder = mutableListOf<String>()
        builder.appendIfPresent(maxAge, "Max-Age=$maxAge")
        builder.appendIfPresent(expires, "Expires=${expires?.let { ZonedDateTime.of(it, ZoneId.of("GMT")).format(RFC822) }}")
        builder.appendIfPresent(domain, "Domain=$domain")
        builder.appendIfPresent(path, "Path=$path")
        builder.appendIfTrue(secure, "secure")
        builder.appendIfTrue(httpOnly, "HttpOnly")
        return builder.joinToString("; ")
    }

    companion object {
        fun parse(cookieValue: String): Cookie? {
            val pair = cookieValue.split("=", limit = 2)
            if (pair.size < 2) return null
            val valueAndAttributes = pair[1].split("; ")
            val attributes = valueAndAttributes.drop(1)
                .map { attrString -> attrString.split("=").let { it[0] to it.getOrNull(1) } }
            return Cookie(pair[0], valueAndAttributes[0].unquoted(),
                attributes.maxAge(), attributes.expires(), attributes.domain(),
                attributes.path(), attributes.secure(), attributes.httpOnly())
        }

        private fun List<Pair<String, String?>>.maxAge(): Long? = find { it.first.equals("Max-Age", true) }?.second?.toLong()
        private fun List<Pair<String, String?>>.expires(): LocalDateTime? = find { it.first.equals("Expires", true) }?.second?.let { LocalDateTime.parse(it, RFC822) }
        private fun List<Pair<String, String?>>.domain(): String? = find { it.first.equals("Domain", true) }?.second
        private fun List<Pair<String, String?>>.path(): String? = find { it.first.equals("Path", true) }?.second
        private fun List<Pair<String, String?>>.secure(): Boolean = find { it.first.equals("secure", true) } != null
        private fun List<Pair<String, String?>>.httpOnly(): Boolean = find { it.first.equals("HttpOnly", true) } != null
    }

    private fun MutableList<String>.appendIfPresent(toCheck: Any?, toInclude: String) {
        if (toCheck != null) add(toInclude)
    }

    private fun MutableList<String>.appendIfTrue(toCheck: Boolean, toInclude: String) {
        if (toCheck) add(toInclude)
    }
}

private val RFC822 = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz")

