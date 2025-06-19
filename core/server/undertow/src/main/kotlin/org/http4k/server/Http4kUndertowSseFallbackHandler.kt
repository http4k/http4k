package org.http4k.server

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.HttpString
import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.toParametersMap
import org.http4k.sse.SseHandler

class Http4kUndertowSseFallbackHandler(private val sse: SseHandler, private val fallback: HttpHandler) : HttpHandler {

    override fun handleRequest(exchange: HttpServerExchange) {
        when {
            exchange.hasEventStreamContentType() -> {
                exchange.asRequest()?.let { request ->
                    with(sse(request)) {
                        when {
                            handled -> {
                                exchange.setStatusCode(status.code)
                                headers.toParametersMap().forEach { (name, values) ->
                                    exchange.responseHeaders.putAll(HttpString(name), values.toList())
                                }
                                Http4kUndertowSseHandler(request, consumer).handleRequest(exchange)
                            }
                            else -> fallback.handleRequest(exchange)
                        }
                    }
                } ?: Response(NOT_IMPLEMENTED).into(exchange)
            }

            else -> fallback.handleRequest(exchange)
        }
    }
}

private fun HttpServerExchange.hasEventStreamContentType() =
    requestHeaders["Accept"]?.any { it.equals(ContentType.TEXT_EVENT_STREAM.value, true) } ?: false

