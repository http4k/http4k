package org.http4k.server

import com.sun.net.httpserver.HttpExchange
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Uri
import org.http4k.core.safeLong

class HttpExchangeHandler(private val handler: HttpHandler) : com.sun.net.httpserver.HttpHandler {
    private fun HttpExchange.populate(httpResponse: Response) {
        httpResponse.headers.forEach { (key, value) -> responseHeaders.add(key, value ?: "") }
        if (requestMethod == "HEAD" || httpResponse.status == NO_CONTENT) {
            sendResponseHeaders(httpResponse.status.code, -1)
        } else {
            sendResponseHeaders(httpResponse.status.code, httpResponse.body.length ?: 0)
            httpResponse.body.stream.use { input -> responseBody.use { input.copyTo(it) } }
        }
    }

    private fun HttpExchange.toRequest() =
        Method.supportedOrNull(requestMethod)
            ?.let {
                Request(it,
                    requestURI.rawQuery?.let { Uri.of(requestURI.rawPath).query(requestURI.rawQuery) }
                        ?: Uri.of(requestURI.rawPath))
                    .body(requestBody, requestHeaders.getFirst("Content-Length").safeLong())
                    .headers(requestHeaders.toList().flatMap { (key, values) -> values.map { key to it } })
                    .source(RequestSource(localAddress.address.hostAddress, localAddress.port))
            }

    override fun handle(exchange: HttpExchange) {
        with(exchange) {
            try {
                populate(toRequest()?.let(handler) ?: Response(NOT_IMPLEMENTED))
            } catch (e: Exception) {
                sendResponseHeaders(500, -1)
            } finally {
                close()
            }
        }
    }
}
