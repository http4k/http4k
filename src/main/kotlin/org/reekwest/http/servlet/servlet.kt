package org.reekwest.http.servlet

import org.reekwest.http.core.Headers
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method
import org.reekwest.http.core.Parameters
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Uri
import java.nio.ByteBuffer
import java.util.*
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

fun HttpHandler.asServlet() = HttpHandlerServlet(this)

class HttpHandlerServlet(private val handler: HttpHandler) : HttpServlet() {
    override fun service(req: HttpServletRequest, resp: HttpServletResponse) =
        transfer(handler(req.asServletRequest()), resp)

    private fun transfer(source: Response, destination: HttpServletResponse): Unit {
        destination.setStatus(source.status.code, source.status.description)
        source.headers.forEach { (key, value) -> destination.addHeader(key, value) }
        source.entity?.let { destination.outputStream.write(it.array()) }
    }

    private fun HttpServletRequest.asServletRequest(): Request =
        Request(Method.valueOf(method), Uri.uri(requestURI + queryString.toQueryString()),
            headerParameters(), ByteBuffer.wrap(inputStream.readBytes()))

    private fun HttpServletRequest.headerParameters(): Headers =
        headerNames.asSequence().fold(listOf(), { a: Parameters, b: String -> a.plus(getHeaders(b).asPairs(b)) })

    private fun Enumeration<String>.asPairs(key: String): Parameters = asSequence().map { key to it }.toList()

    private fun String?.toQueryString(): String = if (this != null && this.isNotEmpty()) "?" + this else ""
}