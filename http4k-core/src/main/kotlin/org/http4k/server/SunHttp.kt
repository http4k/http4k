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
import org.http4k.core.then
import org.http4k.filter.ServerFilters.GracefulShutdown
import java.lang.System.currentTimeMillis
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.Executors.newWorkStealingPool
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.math.max

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

        private val shutdownFilter = GracefulShutdown()
        private val executor = newWorkStealingPool()

        private val server = HttpServer.create(InetSocketAddress(port), 1000)
        override fun start(): Http4kServer {
            val handlerWithGracefulShutdown = shutdownFilter.then(httpHandler)
            return apply {
                server.createContext("/", HttpExchangeHandler(handlerWithGracefulShutdown))
                server.executor = executor
                server.start()
            }
        }

        override fun stop() = apply {
            val gracefulShutdownTimeout = Duration.ofSeconds(10)
            val shutdownDeadline = currentTimeMillis() + gracefulShutdownTimeout.toMillis()

            shutdownFilter.shutdown(gracefulShutdownTimeout)

            executor.shutdown()
            val remainingShutdownTimeInMillis = max(0L, shutdownDeadline - currentTimeMillis())
            executor.awaitTermination(remainingShutdownTimeInMillis, MILLISECONDS)

            server.stop(0)
        }
    }
}
