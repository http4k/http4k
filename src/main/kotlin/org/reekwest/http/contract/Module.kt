package org.reekwest.http.contract

import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.BAD_REQUEST
import org.reekwest.http.core.Status.Companion.NOT_FOUND

typealias Router<T> = (T) -> HttpHandler?
typealias RequestRouter = Router<Request>

interface Module {
    infix fun then(that: Module): Module {
        val thisBinding = toRequestRouter()
        val thatBinding = that.toRequestRouter()

        return object : Module {
            override fun toRequestRouter(): RequestRouter = { req -> thisBinding(req) ?: thatBinding(req) }
        }
    }

    fun toHttpHandler(): HttpHandler {
        val handlerMatcher = toRequestRouter()
        return { req ->
            handlerMatcher(req)?.let {
                try {
                    it(req)
                } catch (e: ContractBreach) {
                    Response(BAD_REQUEST)
                }
            } ?: Response(NOT_FOUND)
        }
    }

    fun toRequestRouter(): RequestRouter
}
