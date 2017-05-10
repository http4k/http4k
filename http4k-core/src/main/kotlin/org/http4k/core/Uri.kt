package org.http4k.core

import org.http4k.appendIfNotBlank
import org.http4k.appendIfPresent

data class Uri(val scheme: String, val userInfo: String, val host: String, val port: Int?, val path: String, val query: String, val fragment: String) {
    companion object {
        private val AUTHORITY = Regex("(?:([^@]+)@)?([^:]+)(?::([\\d]+))?")
        private val RFC3986 = Regex("^(?:([^:/?#]+):)?(?://([^/?#]*))?([^?#]*)(?:\\?([^#]*))?(?:#(.*))?")
        fun of(value: String): Uri {
            val result = RFC3986.matchEntire(value) ?: throw RuntimeException("Invalid Uri: $value")
            val (scheme, authority, path, query, fragment) = result.destructured
            val (userInfo, host, port) = parseAuthority(authority)
            return Uri(scheme, userInfo, host, port, path, query, fragment)
        }

        private fun parseAuthority(authority: String): Triple<String, String, Int?> {
            if(authority.isBlank()) return Triple("", "", null)
            val (userInfo, host, portString) = AUTHORITY.matchEntire(authority)?.destructured ?: throw RuntimeException("Invalid authority: $authority")
            val port = portString.toIntOrNull()
            return Triple(userInfo, host, port)
        }
    }

    val authority = StringBuilder()
        .appendIfNotBlank(userInfo, userInfo, "@")
        .appendIfNotBlank(host, host)
        .appendIfPresent(port, ":", port.toString())
        .toString()

    fun scheme(scheme: String) = copy(scheme = scheme)
    fun userInfo(userInfo: String) = copy(userInfo = userInfo)
    fun host(host: String) = copy(host = host)
    fun port(port: Int?) = copy(port = port)
    fun path(path: String) = copy(path = path)
    fun query(query: String) = copy(query = query)
    fun fragment(fragment: String) = copy(fragment = fragment)

    fun authority(authority: String): Uri {
        val (userInfo, host, port) = parseAuthority(authority)
        return copy(userInfo = userInfo, host = host, port = port)
    }

    override fun toString(): String = StringBuilder()
        .appendIfNotBlank(scheme, scheme, ":")
        .appendIfNotBlank(authority, "//", authority)
        .append(path)
        .appendIfNotBlank(query, "?", query)
        .appendIfNotBlank(fragment, "#", fragment).toString()

}

fun Uri.query(name: String, value: String?): Uri = copy(query = query.toParameters().plus(name to value).toUrlEncoded())