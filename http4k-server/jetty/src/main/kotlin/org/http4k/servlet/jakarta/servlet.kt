package org.http4k.servlet.jakarta

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.http4k.core.Method
import org.http4k.core.Parameters
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.safeLong
import java.util.Enumeration

@Suppress("DEPRECATION")
fun Response.transferTo(destination: HttpServletResponse) {
    destination.setStatus(status.code, status.description)
    headers.forEach { (key, value) -> destination.addHeader(key, value) }
    body.stream.use { input -> destination.outputStream.use { output -> input.copyTo(output) } }
}

fun HttpServletRequest.asHttp4kRequest() =
    Request(Method.valueOf(method), Uri.of(requestURI + queryString.toQueryString()))
        .body(inputStream, getHeader("Content-Length").safeLong()).headers(headerParameters())
        .source(RequestSource(remoteAddr, remotePort, scheme))

private fun HttpServletRequest.headerParameters() =
    headerNames.asSequence().fold(listOf()) { a: Parameters, b: String -> a.plus(getHeaders(b).asPairs(b)) }

private fun Enumeration<String>.asPairs(key: String) = asSequence().map { key to it }.toList()

private fun String?.toQueryString(): String = if (this != null && isNotEmpty()) "?$this" else ""
