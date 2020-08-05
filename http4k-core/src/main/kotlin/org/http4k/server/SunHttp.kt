package org.http4k.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler as SunHttpHandler
import com.sun.net.httpserver.HttpServer
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.safeLong
import java.net.InetSocketAddress

class HttpExchangeHandler(private val handler: HttpHandler): SunHttpHandler {
    private fun HttpExchange.populate(httpResponse: Response) {
        httpResponse.headers.forEach { (key, value) -> responseHeaders.add(key, value) }
        if (requestMethod == "HEAD") {
            sendResponseHeaders(httpResponse.status.code, -1)
        } else {
            sendResponseHeaders(httpResponse.status.code, 0)
            httpResponse.body.stream.use { input -> responseBody.use { input.copyTo(it) } }
        }
    }

    private fun HttpExchange.toRequest(): Request =
        Request(Method.valueOf(requestMethod),
            requestURI.rawQuery?.let { Uri.of(requestURI.rawPath).query(requestURI.rawQuery) }
                ?: Uri.of(requestURI.rawPath))
            .body(requestBody, requestHeaders.getFirst("Content-Length").safeLong())
            .headers(requestHeaders.toList().flatMap { (key, values) -> values.map { key to it } })
            .source(RequestSource(localAddress.address.hostAddress, localAddress.port))

    override fun handle(exchange: HttpExchange) {
        with(exchange) {
            try {
                populate(handler(toRequest()))
            } catch (e: Exception) {
                sendResponseHeaders(500, 0)
            } finally {
                close()
            }
        }
    }
}

data class SunHttp(val port: Int = 8000) : ServerConfig {
    override fun toServer(httpHandler: HttpHandler): Http4kServer = object : Http4kServer {
        override fun port(): Int = if (port > 0) port else server.address.port

        private val server = HttpServer.create(InetSocketAddress(port), 0)
        override fun start(): Http4kServer = apply {
            server.createContext("/", HttpExchangeHandler(httpHandler))
            server.start()
        }

        override fun stop() = apply { server.stop(0) }
    }
}
