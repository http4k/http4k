package org.http4k.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.http4k.core.ClientAddress
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.safeLong
import java.net.InetSocketAddress


data class SunHttp(val port: Int = 8000) : ServerConfig {
    override fun toServer(httpHandler: HttpHandler): Http4kServer = object : Http4kServer {
        override fun port(): Int = if (port > 0) port else server.address.port

        private val server = HttpServer.create(InetSocketAddress(port), 0)
        override fun start(): Http4kServer = apply {
            server.createContext("/") {
                try {
                    it.populate(httpHandler(it.toRequest()))
                } catch (e: Exception) {
                    it.sendResponseHeaders(500, 0)
                }
                it.close()
            }
            server.start()
        }

        override fun stop() = apply { server.stop(0) }
    }
}

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
        .source(RequestSource(ClientAddress(localAddress.address.hostAddress), localAddress.port))
