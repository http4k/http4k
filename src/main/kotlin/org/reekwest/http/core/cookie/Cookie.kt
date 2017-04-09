package org.reekwest.http.core.cookie

import org.reekwest.http.core.Parameters

data class Cookie(val name: String, val value: String, val attributes: Parameters = listOf()) {
    enum class Attribute(val attributeName: String) {
        COMMENT("Comment"),
        DOMAIN("Domain"),
        MAX_AGE("Max-Age"),
        PATH("Path"),
        SECURE("Secure"),
        HTTP_ONLY("HttpOnly"),
        EXPIRES("Expires")
    }

    override fun toString(): String = "$name=${value.quoted()}; ${attributes.cookieString()}"

    private fun Parameters.cookieString(): String = map { "${it.first}=${it.second}" }.joinToString("; ")

    private fun String.quoted() = "\"${this.replace("\"", "\\\"")}\""
}

