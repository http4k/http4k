package org.http4k.server

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.HttpString
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Uri
import org.http4k.core.toParametersMap
import org.http4k.sse.SseHandler

class Http4kSetHeadersHandler(private val next: HttpHandler, private val sse: SseHandler) : HttpHandler {

    override fun handleRequest(exchange: HttpServerExchange) {
        // we probably shouldn't have to deal with request here...
        val (headers, _) = sse(exchange.asRequest() ?: error("Cannot create request from exchange"))
        headers.toParametersMap().forEach { (name, values) ->
            exchange.responseHeaders.putAll(HttpString(name), values.toList())
        }
        next.handleRequest(exchange)
    }

    private fun HttpServerExchange.asRequest() =
        Method.supportedOrNull(requestMethod.toString())?.let {
            Request(it, Uri.of("$relativePath?$queryString"))
                .headers(requestHeaders
                    .flatMap { header -> header.map { header.headerName.toString() to it } })
                .source(RequestSource(sourceAddress.hostString, sourceAddress.port, requestScheme))
        }
}
