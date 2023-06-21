package org.http4k.server

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.HttpString
import org.http4k.core.Headers
import org.http4k.core.toParametersMap

class Http4kSetHeadersHandler(private val next: HttpHandler, private val headers: Headers) : HttpHandler {

    override fun handleRequest(exchange: HttpServerExchange) {
        headers.toParametersMap().forEach { (name, values) ->
            exchange.responseHeaders.putAll(HttpString(name), values.toList())
        }
        next.handleRequest(exchange)
    }

}
