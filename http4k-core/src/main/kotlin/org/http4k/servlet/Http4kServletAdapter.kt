package org.http4k.servlet

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Parameters
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.safeLong
import java.util.Enumeration
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Adapts between the Servlet and http4k APIs
 */
class Http4kServletAdapter(private val handler: HttpHandler) {
    fun handle(req: HttpServletRequest, resp: HttpServletResponse) = handler(req.asHttp4kRequest()).transferTo(resp)
}

@Suppress("DEPRECATION")
private fun Response.transferTo(destination: HttpServletResponse) {
    destination.setStatus(status.code, status.description)
    headers.forEach { (key, value) -> destination.addHeader(key, value) }
    body.stream.use { input -> destination.outputStream.use { output -> input.copyTo(output) } }
}

private fun HttpServletRequest.asHttp4kRequest() =
    Request(Method.valueOf(method), Uri.of(requestURI + queryString.toQueryString()))
        .body(inputStream, getHeader("Content-Length").safeLong()).headers(headerParameters())

private fun HttpServletRequest.headerParameters() =
    headerNames.asSequence().fold(listOf()) { a: Parameters, b: String -> a.plus(getHeaders(b).asPairs(b)) }

private fun Enumeration<String>.asPairs(key: String) = asSequence().map { key to it }.toList()

private fun String?.toQueryString(): String = if (this != null && isNotEmpty()) "?$this" else ""
