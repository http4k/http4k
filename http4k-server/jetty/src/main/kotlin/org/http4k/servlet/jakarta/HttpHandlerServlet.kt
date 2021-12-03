package org.http4k.servlet.jakarta

import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.http4k.core.HttpHandler

/**
 * This is a carbon copy of the standard Servlet classes from the core module, just using the repackaged Jakarta classes
 */
class HttpHandlerServlet(handler: HttpHandler) : HttpServlet() {
    private val adapter = Http4kJakartaServletAdapter(handler)

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) = adapter.handle(req, resp)
}

fun HttpHandler.asServlet() = HttpHandlerServlet(this)

/**
 * Adapts between the Servlet and http4k APIs
 */
class Http4kJakartaServletAdapter(private val handler: HttpHandler) {
    fun handle(req: HttpServletRequest, resp: HttpServletResponse) = handler(req.asHttp4kRequest()).transferTo(resp)
}
