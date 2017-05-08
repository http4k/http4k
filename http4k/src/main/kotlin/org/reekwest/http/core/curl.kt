package org.http4k.http.core

import org.http4k.http.appendIfNotEmpty
import org.http4k.http.appendIfPresent
import org.http4k.http.quoted

fun Request.toCurl(): String =
    StringBuilder("curl")
        .append(" -X $method")
        .appendIfNotEmpty(headers, " " + headers.map { """-H "${it.first}:${it.second}"""" }.joinToString(" "))
        .appendIfPresent(body, " --data ${bodyString().truncated().quoted()}")
        .append(" \"$uri\"")
        .toString()

private fun String.truncated(): String {
    return if (length > 256)
        substring(0..127) + "[truncated]" + substring(length - 128)
    else this
}