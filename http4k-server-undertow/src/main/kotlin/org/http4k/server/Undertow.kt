package org.http4k.server

import io.undertow.Undertow
import io.undertow.UndertowOptions.ENABLE_HTTP2
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.BlockingHandler
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
import org.http4k.filter.ServerFilters.GracefulShutdown
import java.net.InetSocketAddress
import java.time.Duration

/**
 * Exposed to allow for insertion into a customised Undertow server instance
 */
class HttpUndertowHandler(handler: HttpHandler) : io.undertow.server.HttpHandler {
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

data class Undertow(val port: Int = 8000, val enableHttp2: Boolean) : ServerConfig {
    constructor(port: Int = 8000) : this(port, false)

    override fun toServer(httpHandler: HttpHandler): Http4kServer =
        object : Http4kServer {
            val shutdownFilter = GracefulShutdown()

            val server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setServerOption(ENABLE_HTTP2, enableHttp2)
                .setHandler((BlockingHandler(HttpUndertowHandler(shutdownFilter.then(httpHandler))))).build()

            override fun start() = apply { server.start() }

            override fun stop() = apply {
                shutdownFilter.shutdown(Duration.ofSeconds(10))
                server.stop()
            }

            override fun port(): Int = if (port > 0) port else (server.listenerInfo[0].address as InetSocketAddress).port
        }
}
