package org.http4k.bridge

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.http4k.core.HttpHandler
import org.http4k.lens.LensFailure
import org.http4k.servlet.jakarta.asServlet
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.MediaType.ALL_VALUE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod.DELETE
import org.springframework.web.bind.annotation.RequestMethod.GET
import org.springframework.web.bind.annotation.RequestMethod.HEAD
import org.springframework.web.bind.annotation.RequestMethod.OPTIONS
import org.springframework.web.bind.annotation.RequestMethod.PATCH
import org.springframework.web.bind.annotation.RequestMethod.POST
import org.springframework.web.bind.annotation.RequestMethod.PUT
import org.springframework.web.bind.annotation.RequestMethod.TRACE
import org.springframework.web.bind.annotation.ResponseStatus
import java.net.URI

@Controller
@RequestMapping("/")
abstract class SpringToHttp4kFallbackController(http4k: HttpHandler) {
    private val http4kServlet = http4k.asServlet()
    
    @RequestMapping(value = ["**"], method = [GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE])
    fun fallback(request: HttpServletRequest, response: HttpServletResponse) = http4kServlet.service(request, response)
    
    @ExceptionHandler(
        exception = [LensFailure::class],
        produces = [APPLICATION_PROBLEM_JSON_VALUE, APPLICATION_JSON_VALUE, ALL_VALUE]
    )
    @ResponseStatus(BAD_REQUEST)
    fun handleLensFailure(e: LensFailure): ProblemDetail {
        return ProblemDetail.forStatus(BAD_REQUEST).apply {
            type = URI("tag:org.http4k,2025:lens-failure")
            detail = e.message
        }
    }
}

