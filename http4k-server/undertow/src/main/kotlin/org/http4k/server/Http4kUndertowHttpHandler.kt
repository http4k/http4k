package org.http4k.server

import io.undertow.server.HttpServerExchange
import io.undertow.util.HttpString
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Uri
import org.http4k.core.safeLong
import org.http4k.core.then
import org.http4k.core.toParametersMap
import org.http4k.filter.ServerFilters.CatchAll

/**
 * Exposed to allow for insertion into a customised Undertow server instance
 */
class Http4kUndertowHttpHandler(handler: HttpHandler) : io.undertow.server.HttpHandler {
    private val safeHandler = CatchAll().then(handler)

    private fun Response.into(exchange: HttpServerExchange) {
        exchange.statusCode = status.code
        headers.toParametersMap().forEach { (name, values) ->
            exchange.responseHeaders.putAll(HttpString(name), values.toList())
        }
        body.stream.use { it.copyTo(exchange.outputStream) }
    }

    private fun HttpServerExchange.asRequest() =
        Method.supportedOrNull(requestMethod.toString())?.let {
            Request(it, Uri.of("$relativePath?$queryString"), protocol.toString())
                .headers(requestHeaders
                    .flatMap { header -> header.map { header.headerName.toString() to it } })
                .body(inputStream, requestHeaders.getFirst("Content-Length").safeLong())
                .source(RequestSource(sourceAddress.hostString, sourceAddress.port, requestScheme))
        }

    override fun handleRequest(exchange: HttpServerExchange) =
        (exchange.asRequest()?.let { safeHandler(it) } ?: Response(NOT_IMPLEMENTED)).into(exchange)
}
