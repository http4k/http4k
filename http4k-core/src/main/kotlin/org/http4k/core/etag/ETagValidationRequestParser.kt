package org.http4k.core.etag

import java.util.regex.Matcher
import java.util.regex.Pattern

data class ETag(val value: String, val weak: Boolean = false) {
    fun toHeaderString(): String = (if (weak) "W/" else "") + """"$value""""
}

sealed class FieldValue {
    data class ETags(val value: List<ETag>) : FieldValue() {
        constructor(vararg eTags: ETag) : this(eTags.toList())
    }

    object Wildcard : FieldValue()
}

/**
 * Implemented according to https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.11
 */
class ETagValidationRequestParser {
    companion object {
        private const val w = "W/"
        private val eTag = Pattern.compile("^(?:($w)?\"([^\"]+)\")")
        private val separator = Pattern.compile("(?<=\")\\s*,\\s*(?=($w)?\")")

        fun parse(headerValue: String): FieldValue {
            if (headerValue == "*") return FieldValue.Wildcard
            return FieldValue.ETags(headerValue.split(separator).mapNotNull { parseTag(it.trim('\t', ' ')) })
        }

        private fun parseTag(value: String): ETag? {
            val matcher = eTag.matcher(value)
            while (matcher.find()) {
                return ETag(matcher.tag(), matcher.isWeak())
            }
            return null
        }

        private fun Matcher.isWeak(): Boolean = group(1) == w
        private fun Matcher.tag(): String = group(2)
    }
}
