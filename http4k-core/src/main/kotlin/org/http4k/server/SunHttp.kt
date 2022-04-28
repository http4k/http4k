package org.http4k.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Uri
import org.http4k.core.safeLong
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.ServerConfig.UnsupportedStopMode
import java.net.InetSocketAddress
import java.util.concurrent.Executors.newWorkStealingPool
import java.util.concurrent.TimeUnit
import com.sun.net.httpserver.HttpHandler as SunHttpHandler

class HttpExchangeHandler(private val handler: HttpHandler) : SunHttpHandler {
    private fun HttpExchange.populate(httpResponse: Response) {
        httpResponse.headers.forEach { (key, value) -> responseHeaders.add(key, value) }
        if (requestMethod == "HEAD" || httpResponse.status == NO_CONTENT) {
            sendResponseHeaders(httpResponse.status.code, -1)
        } else {
            sendResponseHeaders(httpResponse.status.code, httpResponse.body.length ?: 0)
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
                sendResponseHeaders(500, -1)
            } finally {
                close()
            }
        }
    }
}

class SunHttp(val port: Int = 8000, override val stopMode: StopMode = StopMode.Immediate) : ServerConfig {
    constructor(port: Int = 8000): this(port, StopMode.Immediate)

    override fun toServer(http: HttpHandler): Http4kServer = object : Http4kServer {
        override fun port(): Int = if (port > 0) port else server.address.port

        private val executor = newWorkStealingPool()
        private val server = HttpServer.create(InetSocketAddress(port), 1000)
        override fun start(): Http4kServer = apply {
            server.createContext("/", HttpExchangeHandler(http))
            server.executor = executor
            server.start()
        }

        override fun stop() = apply {
            if (stopMode is StopMode.Graceful) {
                executor.shutdown()
                executor.awaitTermination(stopMode.timeout.toMillis(), TimeUnit.MILLISECONDS)
            }
            server.stop(0)
        }
    }
}
