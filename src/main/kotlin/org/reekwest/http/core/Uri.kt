package org.reekwest.http.core

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

    fun StringBuilder.appendIfNotBlank(valueToCheck: String, vararg toAppend: String): StringBuilder {
        if (valueToCheck.isNotBlank()) toAppend.forEach { append(it) }
        return this
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