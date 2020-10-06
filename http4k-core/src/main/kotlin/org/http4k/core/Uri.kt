package org.http4k.core

import org.http4k.appendIfNotBlank
import org.http4k.appendIfPresent
import java.net.URI
import java.net.URLDecoder

data class Uri(val scheme: String, val userInfo: String, val host: String, val port: Int?, val path: String, val query: String, val fragment: String) : Comparable<Uri> {

    companion object {
        private val AUTHORITY = Regex("(?:([^@]+)@)?([^:]+)(?::([\\d]+))?")
        private val RFC3986 = Regex("^(?:([^:/?#]+):)?(?://([^/?#]*))?([^?#]*)(?:\\?([^#]*))?(?:#(.*))?")

        @JvmStatic
        fun of(value: String): Uri {
            val result = RFC3986.matchEntire(value) ?: throw RuntimeException("Invalid Uri: $value")
            val (scheme, authority, path, query, fragment) = result.destructured
            val (userInfo, host, port) = parseAuthority(authority)
            return Uri(scheme, userInfo, host, port, path, query, fragment)
        }

        private fun parseAuthority(authority: String): Triple<String, String, Int?> = when {
            authority.isBlank() -> Triple("", "", null)
            else -> {
                val (userInfo, host, portString) = AUTHORITY.matchEntire(authority)?.destructured
                    ?: throw RuntimeException("Invalid authority: $authority")
                val port = portString.toIntOrNull()
                Triple(userInfo, host, port)
            }
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

    fun authority(authority: String): Uri = parseAuthority(authority).let { (userInfo, host, port) ->
        copy(userInfo = userInfo, host = host, port = port)
    }

    override fun compareTo(other: Uri) = toString().compareTo(other.toString())

    override fun toString() = StringBuilder()
        .appendIfNotBlank(scheme, scheme, ":")
        .appendIfNotBlank(authority, "//", authority)
        .append(
            when {
                authority.isBlank() -> path
                path.isBlank() || path.startsWith("/") -> path
                else -> "/$path"
            }
        )
        .appendIfNotBlank(query, "?", query)
        .appendIfNotBlank(fragment, "#", fragment).toString()
}

fun Uri.removeQuery(name: String) = copy(query = query.toParameters().filterNot { it.first == name }.toUrlFormEncoded())

fun Uri.query(name: String, value: String?): Uri = copy(query = query.toParameters().plus(name to value).toUrlFormEncoded())

fun String.toPathEncoded(): String = URI("http", null, "/$this", null).toURL().path.drop(1).replace("/", "%2F")

fun String.fromPathEncoded(): String = URLDecoder.decode(this, "UTF-8")

fun Uri.extend(uri: Uri): Uri =
    appendToPath(uri.path).copy(query = (query.toParameters() + uri.query.toParameters()).toUrlFormEncoded())

private fun Uri.appendToPath(newPath: String): Uri =
    if (newPath == "") this
    else copy(path = (path.removeSuffix("/") + "/" + newPath.removePrefix("/")))
