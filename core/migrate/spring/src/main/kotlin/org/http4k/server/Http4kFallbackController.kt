package org.http4k.server

import org.http4k.core.HttpHandler
import org.http4k.servlet.asServlet
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.DELETE
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.HEAD
import org.springframework.web.bind.annotation.RequestMethod.OPTIONS
import org.springframework.web.bind.annotation.RequestMethod.PATCH
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RequestMethod.PUT
import org.springframework.web.bind.annotation.RequestMethod.TRACE
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
@RequestMapping("/")
abstract class Http4kFallbackController(http4k: HttpHandler) {
    private val asServlet = http4k.asServlet()

    @RequestMapping(value = ["**"], method = [GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE])
    fun fallback(request: HttpServletRequest, response: HttpServletResponse) {
        asServlet.service(request, response)
    }
}
