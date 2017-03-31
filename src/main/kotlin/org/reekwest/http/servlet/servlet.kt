package org.reekwest.http.servlet

import org.reekwest.http.core.Entity
import org.reekwest.http.core.Headers
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Uri
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
        source.entity?.let { destination.outputStream.write(it.value) }
    }

    private fun HttpServletRequest.asServletRequest(): Request =
        Request(Method.valueOf(method), Uri.uri(requestURI + queryString.toQueryString()),
            headerParameters(), Entity(inputStream.readBytes())
        )

    private fun HttpServletRequest.headerParameters(): Headers = headerNames.asSequence().map { it to this.getHeader(it) }.toList()

    private fun String?.toQueryString(): String = if (this != null && this.isNotEmpty()) "?" + this else ""
}