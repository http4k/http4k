package org.http4k.server

import io.undertow.server.HttpServerExchange
import io.undertow.util.HttpString
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.safeLong
import org.http4k.core.then
import org.http4k.core.toParametersMap
import org.http4k.filter.ServerFilters

/**
 * Exposed to allow for insertion into a customised Undertow server instance
 */
class Http4kUndertowHttpHandler(handler: HttpHandler) : io.undertow.server.HttpHandler {
    private val safeHandler = ServerFilters.CatchAll().then(handler)

    private fun Response.into(exchange: HttpServerExchange) {
        exchange.statusCode = status.code
        headers.toParametersMap().forEach { (name, values) ->
            exchange.responseHeaders.putAll(HttpString(name), values.toList())
        }
        body.stream.use { it.copyTo(exchange.outputStream) }
    }

    private fun HttpServerExchange.asRequest(): Request =
        Request(Method.valueOf(requestMethod.toString()), Uri.of("$relativePath?$queryString"))
            .headers(requestHeaders
                .flatMap { header -> header.map { header.headerName.toString() to it } })
            .body(inputStream, requestHeaders.getFirst("Content-Length").safeLong())
            .source(RequestSource(sourceAddress.hostString, sourceAddress.port, requestScheme))

    override fun handleRequest(exchange: HttpServerExchange) = safeHandler(exchange.asRequest()).into(exchange)
}
