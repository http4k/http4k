package org.http4k.server

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.HttpString
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.toParametersMap
import org.http4k.sse.SseHandler
import org.http4k.sse.encodingHeaders
import org.http4k.sse.streamHeaders
import org.http4k.sse.streamTo
import java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor

class Http4kUndertowSseFallbackHandler(
    private val sse: SseHandler,
    private val undertowHandler: HttpHandler,
    private val http4kFallback: org.http4k.core.HttpHandler
) : HttpHandler {
    private val executor = newVirtualThreadPerTaskExecutor()

    override fun handleRequest(exchange: HttpServerExchange) {
        when {
            exchange.hasEventStreamContentType() -> {
                exchange.asRequest()?.let { request ->
                    with(sse(request)) {
                        when {
                            handled -> {
                                exchange.setStatusCode(status.code)
                                (headers + encodingHeaders).toParametersMap().forEach { (name, values) ->
                                    exchange.responseHeaders.putAll(HttpString(name), values.toList())
                                }
                                when (contentEncoding) {
                                    null -> Http4kUndertowSseHandler(request, consumer).handleRequest(exchange)

                                    else -> {
                                        streamHeaders.forEach { (name, value) -> exchange.responseHeaders.put(HttpString(name), value) }
                                        exchange.dispatch(executor, HttpHandler {
                                            it.startBlocking()
                                            streamTo(request, it.outputStream)
                                            it.endExchange()
                                        })
                                    }
                                }
                            }

                            else -> http4kFallback(request).into(exchange)
                        }
                    }
                } ?: Response(NOT_IMPLEMENTED).into(exchange)
            }

            else -> undertowHandler.handleRequest(exchange)
        }
    }
}

private fun HttpServerExchange.hasEventStreamContentType() =
    requestHeaders["Accept"]?.any { it.contains(TEXT_EVENT_STREAM.value, true) } ?: false
