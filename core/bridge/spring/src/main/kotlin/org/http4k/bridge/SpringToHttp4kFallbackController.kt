package org.http4k.bridge

import org.http4k.core.HttpHandler
import org.http4k.servlet.jakarta.asServlet
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
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@Controller
@RequestMapping("/")
abstract class SpringToHttp4kFallbackController(http4k: HttpHandler) {
    private val http4kServlet = http4k.asServlet()

    @RequestMapping(value = ["**"], method = [GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE])
    fun fallback(request: HttpServletRequest, response: HttpServletResponse) = http4kServlet.service(request, response)
}
