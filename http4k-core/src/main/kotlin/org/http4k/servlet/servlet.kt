package org.http4k.servlet

import org.http4k.core.Headers
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Parameters
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.safeLong
import java.util.Enumeration
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

fun HttpHandler.asServlet() = HttpHandlerServlet(this)

class HttpHandlerServlet(private val handler: HttpHandler) : HttpServlet() {
    override fun service(req: HttpServletRequest, resp: HttpServletResponse) = handler(req.asHttp4kRequest()).transferTo(resp)
}

@Suppress("DEPRECATION")
private fun Response.transferTo(destination: HttpServletResponse) {
    destination.setStatus(status.code, status.description)
    headers.forEach { (key, value) -> destination.addHeader(key, value) }
    body.stream.use { input -> destination.outputStream.use { output -> input.copyTo(output) } }
}

private fun HttpServletRequest.asHttp4kRequest(): Request =
    Request(Method.valueOf(method), Uri.of(requestURI + queryString.toQueryString()))
        .body(inputStream, getHeader("Content-Length").safeLong()).headers(headerParameters())

private fun HttpServletRequest.headerParameters(): Headers =
    headerNames.asSequence().fold(listOf()) { a: Parameters, b: String -> a.plus(getHeaders(b).asPairs(b)) }

private fun Enumeration<String>.asPairs(key: String): Parameters = asSequence().map { key to it }.toList()

private fun String?.toQueryString(): String = if (this != null && isNotEmpty()) "?" + this else ""
