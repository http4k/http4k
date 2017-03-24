package com.gourame.http.core

data class Uri(val scheme: String, val authority: String, val path: String, val query: String, val fragment: String) {
    companion object {
        private val AUTHORITY = Regex("(?:([^@]+)@)?([^:]+)(?::([\\d]+))?")
        private val RFC3986 = Regex("^(?:([^:/?#]+):)?(?://([^/?#]*))?([^?#]*)(?:\\?([^#]*))?(?:#(.*))?")
        fun uri(value: String): Uri {
            val result = RFC3986.matchEntire(value) ?: throw RuntimeException("Invalid Uri: $value")
            return Uri(scheme = result.groupValues[1],
                authority = result.groupValues[2],
                path = result.groupValues[3],
                query = result.groupValues[4],
                fragment = result.groupValues[5])
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append(scheme).append(":")
        builder.append("//").append(authority)
        builder.append(path)
        if (query.isNotBlank()) {
            builder.append("?").append(query)
        }
        if (fragment.isNotBlank()) {
            builder.append("#").append(fragment)
        }
        return builder.toString()
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