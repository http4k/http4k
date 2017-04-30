package org.reekwest.http.core.cookie

import org.reekwest.http.core.Parameters
import org.reekwest.http.quoted

data class Cookie(val name: String, val value: String, val attributes: Parameters = listOf()) {
    override fun toString(): String = "$name=${value.quoted()}; ${attributes.cookieString()}"

    private fun Parameters.cookieString(): String = map { "${it.first}=${it.second}" }.joinToString("; ")

    companion object {
        fun parse(cookieValue: String): Cookie? {
            "(.+)=\"(.+)\"; (.*)".toRegex().matchEntire(cookieValue)?.let {
                val (name, value, attributesString) = it.destructured
                val attrib = attributesString.split("; ")
                    .filter { it.contains("=") }
                    .map { attrString -> attrString.split("=").let { it[0] to it[1] } }
                return Cookie(name, value, attrib)
            }
            return null
        }
    }
}

