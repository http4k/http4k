package org.http4k.servlet

import org.http4k.core.Headers
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Parameters
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.util.*
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

fun HttpHandler.asServlet() = HttpHandlerServlet(this)

class HttpHandlerServlet(private val handler: HttpHandler) : HttpServlet() {
    override fun service(req: HttpServletRequest, resp: HttpServletResponse) =
        transfer(handler(req.asServletRequest()), resp)

    @Suppress("DEPRECATION")
    private fun transfer(source: Response, destination: HttpServletResponse): Unit {
        destination.setStatus(source.status.code, source.status.description)
        source.headers.forEach { (key, value) -> destination.addHeader(key, value) }
        source.body?.let { Channels.newChannel(destination.outputStream).write(it) }
    }

    private fun HttpServletRequest.asServletRequest(): Request =
        Request(Method.valueOf(method), Uri.of(requestURI + queryString.toQueryString()),
            headerParameters(), ByteBuffer.wrap(inputStream.readBytes()))

    private fun HttpServletRequest.headerParameters(): Headers =
        headerNames.asSequence().fold(listOf(), { a: Parameters, b: String -> a.plus(getHeaders(b).asPairs(b)) })

    private fun Enumeration<String>.asPairs(key: String): Parameters = asSequence().map { key to it }.toList()

    private fun String?.toQueryString(): String = if (this != null && this.isNotEmpty()) "?" + this else ""
}