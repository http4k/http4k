package org.http4k.core

import org.http4k.appendIfNotBlank
import org.http4k.appendIfPresent
import org.http4k.urlDecoded
import org.http4k.urlEncoded

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
        .append(when {
            authority.isBlank() -> path
            path.isBlank() || path.startsWith("/") -> path
            else -> "/$path"
        })
        .appendIfNotBlank(query, "?", query)
        .appendIfNotBlank(fragment, "#", fragment).toString()
}

fun Uri.removeQuery(name: String) = copy(query = query.toParameters().filterNot { it.first == name }.toUrlFormEncoded())

fun Uri.removeQueries(prefix: String= "") =
    copy(query = query.toParameters().filterNot { it.first.startsWith(prefix) }.toUrlFormEncoded())

fun Uri.query(name: String, value: String?): Uri =
    copy(query = query.toParameters().plus(name to value).toUrlFormEncoded())

fun Uri.queryParametersEncoded(): Uri =
    copy(query = query.toParameters().toUrlFormEncoded())

/**
 * @see [RFC 3986, appendix A](https://www.ietf.org/rfc/rfc3986.txt)
 */
private val validPathSegmentChars = setOf(
    '~', '-', '.', '_',                                // unreserved
    '!', '$', '&', '\'', '(', ')', '+', ',', ';', '=', // sub-delims
    ':', '@'                                           // valid
)

private fun Char.isAsciiLetter() = this in 'a'..'z' || this in 'A'..'Z'

private fun Char.isValidSpecialPathSegmentChar() = validPathSegmentChars.contains(this)

fun String.toPathSegmentEncoded(): String =
    this.map {
        when {
            it.isAsciiLetter() || it.isDigit() || it.isValidSpecialPathSegmentChar() -> it
            it.isWhitespace() -> "%20"
            else -> it.toString().urlEncoded()
        }
    }.joinToString(separator = "")

fun String.toPathSegmentDecoded(): String =
    this.replace("+", "%2B").urlDecoded()

fun Uri.extend(uri: Uri): Uri =
    appendToPath(uri.path).copy(query = (query.toParameters() + uri.query.toParameters()).toUrlFormEncoded(),
        fragment = uri.fragment.takeIf { it.isNotEmpty() } ?: fragment)

fun Uri.appendToPath(pathToAppend: String?): Uri =
    if (pathToAppend.isNullOrBlank()) this
    else copy(path = (path.removeSuffix("/") + "/" + pathToAppend.removePrefix("/")))

fun Uri.relative(relative: String): Uri = if (relative == "") this else this.relative(Uri.of(relative))

// Implementation of relative resolution as per [RFC3986 5.2](https://datatracker.ietf.org/doc/html/rfc3986#section-5.2)
fun Uri.relative(relative: Uri): Uri {
    fun String.merge(relativePath: String): String {
        return when {
            this == "" -> if (relativePath[0] == '/') relativePath else "/$relativePath"
            else -> lastIndexOf('/').let { this.slice(0..(it)) } + relativePath
        }
    }

    return when {
        relative.scheme != "" -> relative
        relative.authority != "" -> Uri(scheme, relative.userInfo, relative.host, relative.port, relative.path, relative.query, relative.fragment)
        relative.path == "" -> Uri(scheme, userInfo, host, port, path, if (relative.query == "") query else relative.query, relative.fragment)
        relative.path.startsWith("/") -> Uri(scheme, userInfo, host, port, relative.path.normalizePath(), relative.query, relative.fragment)
        else -> Uri(scheme, userInfo, host, port, path.merge(relative.path).normalizePath(), relative.query, relative.fragment)
    }
}

private fun String.normalizePath(): String {
    fun String.replacePrefix(original: String, newPrefix: String): String = newPrefix + (removePrefix(original))
    fun String.removeLastSegment(): String = lastIndexOf('/').let { this.slice(0..<it) }
    fun String.secondIndexOf(char: Char): Int = indexOf(char, indexOf(char) + 1)
    fun String.firstSegment() = when {
        startsWith("/") -> if (secondIndexOf('/') == -1) this else slice(0..<secondIndexOf('/'))
        else -> if (indexOf('/') == -1) this else slice(0..<indexOf('/'))
    }

    var input = this
    var output = ""

    while (input.isNotBlank()) {
        when {
            input.startsWith("./") -> input = input.removePrefix("./")
            input.startsWith("../") -> input = input.removePrefix("../")
            input.startsWith("/./") -> input = input.replacePrefix("/./", "/")
            input.startsWith("/../") -> {
                input = input.replacePrefix("/../", "/")
                output = output.removeLastSegment()
            }
            input.firstSegment() == "/.." -> {
                input = input.replacePrefix("/..", "/")
                output = output.removeLastSegment()
            }

            input.firstSegment() == "/." -> input = input.replacePrefix("/.", "/")

            input == "." -> input = ""
            input == ".." -> input = ""
            else -> {
                val newFirstSegment: String = input.firstSegment()

                input = input.removePrefix(newFirstSegment)
                output += newFirstSegment
            }
        }
    }

    return output
}
