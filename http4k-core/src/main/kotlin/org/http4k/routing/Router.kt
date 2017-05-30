package org.http4k.routing

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.LensFailure

interface Router {
    fun match(request: Request): HttpHandler?

    fun then(that: Router): Router {
        val originalMatch = this::match
        return object : Router {
            override fun match(request: Request): HttpHandler? = originalMatch(request) ?: that.match(request)
        }
    }

    fun toHttpHandler(): HttpHandler = {
        try {
            match(it)?.invoke(it) ?: Response(Status.NOT_FOUND)
        } catch (e: LensFailure) {
            Response(Status.BAD_REQUEST)
        }
    }
}