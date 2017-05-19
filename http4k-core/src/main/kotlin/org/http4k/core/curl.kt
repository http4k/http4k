package org.http4k.core

import org.http4k.appendIfNotBlank
import org.http4k.appendIfNotEmpty
import org.http4k.quoted

fun Request.toCurl(): String =
    StringBuilder("curl")
        .append(" -X $method")
        .appendIfNotEmpty(headers, " " + headers.map { """-H ${(it.first + ":" + it.second).quoted()}""" }.joinToString(" "))
        .appendIfNotBlank(bodyString(), " --data ${body.toString().truncated().quoted()}")
        .append(" \"$uri\"")
        .toString()

private fun String.truncated(): String {
    return if (length > 256)
        substring(0..127) + "[truncated]" + substring(length - 128)
    else this
}