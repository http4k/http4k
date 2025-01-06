package org.http4k.bridge

import org.http4k.core.HttpHandler
import org.http4k.servlet.asServlet
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
@RequestMapping("/")
abstract class Http4kFallbackController(http4k: HttpHandler) {
    private val http4kServlet = http4k.asServlet()

    @RequestMapping(
        value = ["**"],
        method = [RequestMethod.GET, RequestMethod.HEAD, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.TRACE]
    )
    fun fallback(request: HttpServletRequest, response: HttpServletResponse) = http4kServlet.service(request, response)
}
