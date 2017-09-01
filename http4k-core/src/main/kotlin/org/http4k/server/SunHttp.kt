package org.http4k.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import java.net.InetSocketAddress

data class SunHttp(val port: Int = 8000) : ServerConfig {
    override fun toServer(handler: HttpHandler): Http4kServer {
        return object : Http4kServer {
            private val server = HttpServer.create(InetSocketAddress(port), 0)
            override fun start(): Http4kServer {
                server.createContext("/") {
                    try {
                        it.populate(handler(it.toRequest()))
                    } catch (e: Exception) {
                        it.sendResponseHeaders(500, 0)
                    }
                    it.close()
                }
                server.start()
                return this
            }

            override fun stop() = server.stop(0)
        }
    }
}

private fun HttpExchange.populate(httpResponse: Response) {
    httpResponse.headers.forEach { (key, value) -> responseHeaders.add(key, value) }
    sendResponseHeaders(httpResponse.status.code, 0)
    httpResponse.body.stream.copyTo(responseBody)
}

private fun HttpExchange.toRequest(): Request {
    val uri = requestURI.rawQuery?.let { Uri.of(requestURI.rawPath).query(requestURI.rawQuery) } ?: Uri.of(requestURI.rawPath)
    Request(Method.valueOf(requestMethod), uri)
        .body(requestBody).let {
        return requestHeaders.toList().fold(it) {
            memo, (name, values) ->
            values.fold(memo) { memo2, value -> memo2.header(name, value) }
        }
    }

}
