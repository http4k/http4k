package com.gourame.http.servlet

import com.gourame.http.core.Entity
import com.gourame.http.core.Headers
import com.gourame.http.core.HttpHandler
import com.gourame.http.core.Method
import com.gourame.http.core.Request
import com.gourame.http.core.Response
import com.gourame.http.core.Uri
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
        destination.outputStream.write(source.entity.value)
    }

    private fun HttpServletRequest.asServletRequest(): Request =
        Request(Method.valueOf(method), Uri.Companion.uri(requestURI + queryString.toQueryString().orEmpty()),
            headerParameters(), Entity(inputStream.readBytes())
        )

    private fun HttpServletRequest.headerParameters(): Headers = this.headerNames.asSequence().map { it to this.getHeader(it) }.toMap()

    private fun String?.toQueryString(): String = if (this != null && this.isNotEmpty()) "?" + this else ""
}