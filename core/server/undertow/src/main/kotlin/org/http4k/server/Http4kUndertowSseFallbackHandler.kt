package org.http4k.server

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.HttpString
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Uri
import org.http4k.core.safeLong
import org.http4k.core.toParametersMap
import org.http4k.sse.SseHandler

class Http4kUndertowSseFallbackHandler(private val sse: SseHandler, private val fallback: HttpHandler) : HttpHandler {

    override fun handleRequest(exchange: HttpServerExchange) {
        val request = exchange.asRequest() ?: error("Cannot create request from exchange")

        when {
            exchange.hasEventStreamContentType() -> {
                with(sse(request)) {
                    if (handled) {
                        exchange.setStatusCode(status.code)
                        headers.toParametersMap().forEach { (name, values) ->
                            exchange.responseHeaders.putAll(HttpString(name), values.toList())
                        }
                        Http4kUndertowSseHandler(request, consumer).handleRequest(exchange)
                    } else {
                        fallback.handleRequest(exchange)
                    }
                }
            }

            else -> fallback.handleRequest(exchange)
        }
    }

    private fun HttpServerExchange.asRequest() =
        Method.supportedOrNull(requestMethod.toString())?.let {
            Request(it, Uri.of("$relativePath?$queryString"))
                .headers(requestHeaders
                    .flatMap { header -> header.map { header.headerName.toString() to it } })
                .source(RequestSource(sourceAddress.hostString, sourceAddress.port, requestScheme))
                .body(startBlocking().inputStream, requestHeaders.getFirst("Content-Length").safeLong())
        }
}

private fun HttpServerExchange.hasEventStreamContentType() =
    requestHeaders["Accept"]?.any { it.equals(ContentType.TEXT_EVENT_STREAM.value, true) } ?: false

