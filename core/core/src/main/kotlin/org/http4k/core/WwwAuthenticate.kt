package org.http4k.core

data class WwwAuthenticate(val token: String, val contents: Map<String, String>) : Map<String, String> by contents {

    fun toHeaderValue() = "$token ${contents.entries.joinToString(", ") { "${it.key}=\"${it.value}\"" }}".trim()

    companion object {
        fun parseHeader(headerValue: String): WwwAuthenticate {
            val parts = headerValue.split(" ", limit = 2)
            val token = parts[0]
            val contents = parts.getOrNull(1)?.split(",")
                ?.filter(String::isNotBlank)
                ?.associate {
                    val (key, value) = it.split("=", limit = 2)
                    key.trim() to value.trim().removeSurrounding("\"")
                } ?: emptyMap()
            return WwwAuthenticate(token.trim(), contents)
        }
    }
}
