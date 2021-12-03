package org.http4k.servlet

import org.http4k.core.HttpHandler
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Simple servlet which wraps an http4k HttpHandler
 */
class HttpHandlerServlet(handler: HttpHandler) : HttpServlet() {
    private val adapter = Http4kServletAdapter(handler)

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) = adapter.handle(req, resp)
}

fun HttpHandler.asServlet() = HttpHandlerServlet(this)
