package org.reekwest.http.core.cookie

import org.reekwest.http.core.Parameters
import org.reekwest.http.quoted
import org.reekwest.http.unquoted

data class Cookie(val name: String, val value: String, val attributes: Parameters = listOf()) {
    override fun toString(): String = "$name=${value.quoted()}; ${attributes.cookieString()}"

    private fun Parameters.cookieString(): String = map { "${it.first}=${it.second}" }.joinToString("; ")

    companion object {
        fun parse(cookieValue: String): Cookie? {
            val pair = cookieValue.split("=", limit = 2)
            if (pair.size < 2) return null
            val valueAndAttributes = pair[1].split("; ")
            val attrib = valueAndAttributes.drop(1)
                .filter { it.contains("=") }
                .map { attrString -> attrString.split("=").let { it[0] to it[1] } }
            return Cookie(pair[0], valueAndAttributes[0].unquoted(), attrib)
        }
    }
}

