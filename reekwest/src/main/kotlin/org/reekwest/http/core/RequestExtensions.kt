package org.reekwest.http.core

import org.reekwest.http.appendIfNotEmpty
import org.reekwest.http.appendIfPresent
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Method.POST
import org.reekwest.http.core.Method.PUT
import org.reekwest.http.core.Uri.Companion.uri
import org.reekwest.http.core.body.bodyString
import org.reekwest.http.quoted

fun get(uri: String, headers: Headers = listOf(), body: Body? = null) = Request(GET, uri(uri), headers, body)

fun post(uri: String, headers: Headers = listOf(), body: Body? = null) = Request(POST, uri(uri), headers, body)

fun put(uri: String, headers: Headers = listOf(), body: Body? = null) = Request(PUT, uri(uri), headers, body)

fun Request.query(name: String, value: String) = copy(uri = uri.query(name, value))

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