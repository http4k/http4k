package org.http4k.routing

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.lens.LensFailure

typealias Router = (Request) -> HttpHandler?

interface Module {
    infix fun then(that: Module): Module {
        val thisBinding = toRouter()
        val thatBinding = that.toRouter()

        return object : Module {
            override fun toRouter(): Router = { req -> thisBinding(req) ?: thatBinding(req) }
        }
    }

    fun toHttpHandler(): HttpHandler = toRouter().let { router ->
        { req ->
            try {
                router(req)?.invoke(req) ?: Response(NOT_FOUND)
            } catch (e: LensFailure) {
                Response(BAD_REQUEST)
            }
        }
    }

    fun toRouter(): Router
}
