package org.reekwest.http.core.cookie

import org.reekwest.http.core.Parameters
import org.reekwest.http.quoted

data class Cookie(val name: String, val value: String, val attributes: Parameters = listOf()) {
    override fun toString(): String = "$name=${value.quoted()}; ${attributes.cookieString()}"

    private fun Parameters.cookieString(): String = map { "${it.first}=${it.second}" }.joinToString("; ")

}

