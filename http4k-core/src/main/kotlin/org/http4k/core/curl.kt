package org.http4k.core

import org.http4k.appendIfNotBlank
import org.http4k.appendIfNotEmpty
import org.http4k.quoted

fun Request.toCurl(truncateBodyLength: Int = 256): String =
    StringBuilder("curl")
        .append(" -X $method")
        .appendIfNotEmpty(headers, " " + headers.joinToString(" ") { """-H ${(it.first + ":" + it.second).quoted()}""" })
        .appendIfNotBlank(body.toString(), " --data ${body.toString().truncated(truncateBodyLength).quoted()}")
        .append(" \"$uri\"")
        .toString()

private fun String.truncated(truncateBodyLength: Int): String = if (length > truncateBodyLength)
    substring(0..127) + "[truncated]" + substring(length - 128)
else this
