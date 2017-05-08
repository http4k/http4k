package org.http4k.http.core

import org.http4k.http.appendIfNotBlank

data class Uri(val scheme: String, val authority: String, val path: String, val query: String, val fragment: String) {
    companion object {
        private val AUTHORITY = Regex("(?:([^@]+)@)?([^:]+)(?::([\\d]+))?")
        private val RFC3986 = Regex("^(?:([^:/?#]+):)?(?://([^/?#]*))?([^?#]*)(?:\\?([^#]*))?(?:#(.*))?")
        fun uri(value: String): Uri {
            val result = RFC3986.matchEntire(value) ?: throw RuntimeException("Invalid Uri: $value")
            val (scheme, authority, path, query, fragment) = result.destructured
            return Uri(scheme, authority, path, query, fragment)
        }
    }

    override fun toString(): String {
        return StringBuilder()
            .appendIfNotBlank(scheme, scheme, ":")
            .appendIfNotBlank(authority, "//", authority)
            .append(path)
            .appendIfNotBlank(query, "?", query)
            .appendIfNotBlank(fragment, "#", fragment).toString()
    }

    val host: String by lazy {
        AUTHORITY.matchEntire(authority)?.let { it.groupValues[2] }.orEmpty()
    }

    val port: Int? by lazy {
        AUTHORITY.matchEntire(authority)?.let {
            val portValue = it.groupValues[3]
            if (portValue.isNotBlank()) portValue.toInt() else null
        }
    }
}

fun Uri.query(name: String, value: String?): Uri = copy(query = query.toParameters().plus(name to value).toUrlEncoded())