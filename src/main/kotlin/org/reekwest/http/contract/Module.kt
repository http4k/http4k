package org.reekwest.http.contract

import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.BAD_REQUEST
import org.reekwest.http.core.Status.Companion.NOT_FOUND

typealias HandlerMatcher = (Request) -> HttpHandler?

interface Module {
    infix fun then(that: Module): Module {
        val thisBinding = toHandlerMatcher()
        val thatBinding = that.toHandlerMatcher()

        return object : Module {
            override fun toHandlerMatcher(): HandlerMatcher = { req -> thisBinding(req) ?: thatBinding(req) }
        }
    }

    fun toHttpHandler(): HttpHandler {
        val svcBinding = toHandlerMatcher()
        return { req ->
            svcBinding(req)?.let {
                try {
                    it(req)
                } catch (e: ContractBreach) {
                    Response(BAD_REQUEST)
                }
            } ?: Response(NOT_FOUND)
        }
    }

    fun toHandlerMatcher(): HandlerMatcher
}
