package org.reekwest.http.contract.spike

import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.NOT_FOUND

typealias Router<T> = (T) -> HttpHandler?
typealias RequestRouter = Router<Request>

interface Module {
    infix fun then(that: Module): Module {
        val thisRouter = toRequestRouter()
        val thatRouter = that.toRequestRouter()

        return object : Module {
            override fun toRequestRouter(): RequestRouter = { req -> thisRouter(req) ?: thatRouter(req) }
        }
    }

    fun toHttpHandler(): HttpHandler {
        val router = toRequestRouter()
        return { req ->
            router(req)?.let { it(req) } ?: Response(NOT_FOUND)
        }
    }

    fun toRequestRouter(): RequestRouter
}
