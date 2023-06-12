package org.http4k.servlet.jakarta

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Parameters
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Uri
import org.http4k.core.safeLong
import org.http4k.server.supportedOrNull
import java.util.Enumeration

/**
 * Adapts between the Servlet and http4k APIs
 */
class Http4kJakartaServletAdapter(private val handler: HttpHandler) {
    fun handle(req: HttpServletRequest, resp: HttpServletResponse) {
        (req.asHttp4kRequest()?.let(handler) ?: Response(NOT_IMPLEMENTED)).transferTo(resp)
    }
}

fun Response.transferTo(destination: HttpServletResponse) {
    destination.status = status.code
    headers.forEach { (key, value) -> destination.addHeader(key, value) }
    body.stream.use { input -> destination.outputStream.use { output -> input.copyTo(output) } }
}

fun HttpServletRequest.asHttp4kRequest() = Method.supportedOrNull(method)?.let {
    Request(it, Uri.of(requestURI + queryString.toQueryString()))
        .body(inputStream, getHeader("Content-Length").safeLong()).headers(headerParameters())
        .source(RequestSource(remoteAddr, remotePort, scheme))
}

private fun HttpServletRequest.headerParameters() =
    headerNames.asSequence().fold(listOf()) { a: Parameters, b: String -> a.plus(getHeaders(b).asPairs(b)) }

private fun Enumeration<String>.asPairs(key: String) = asSequence().map { key to it }.toList()

private fun String?.toQueryString(): String = if (!isNullOrEmpty()) "?$this" else ""
