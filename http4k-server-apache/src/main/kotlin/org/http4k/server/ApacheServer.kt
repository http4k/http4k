package org.http4k.server

import org.apache.http.Header
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.config.SocketConfig
import org.apache.http.entity.InputStreamEntity
import org.apache.http.impl.bootstrap.ServerBootstrap
import org.apache.http.impl.io.EmptyInputStream
import org.apache.http.protocol.HttpRequestHandler
import org.http4k.core.Headers
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.safeLong
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import java.util.concurrent.TimeUnit

/**
 * Exposed to allow for insertion into a customised Apache WebServer instance
 */
class Http4kRequestHandler(handler: HttpHandler) : HttpRequestHandler {

    private val safeHandler = ServerFilters.CatchAll().then(handler)

    override fun handle(request: org.apache.http.HttpRequest,
                        response: org.apache.http.HttpResponse,
                        context: org.apache.http.protocol.HttpContext) {
        safeHandler(request.asHttp4kRequest()).into(response)
    }

    private fun org.apache.http.HttpRequest.asHttp4kRequest(): Request {
        val request = Request(Method.valueOf(requestLine.method), requestLine.uri)
                .headers(allHeaders.toHttp4kHeaders())
        return when (this) {
            is HttpEntityEnclosingRequest -> request.body(entity.content, getFirstHeader("Content-Length")?.value.safeLong())
            else -> request.body(EmptyInputStream.INSTANCE, 0)
        }
    }

    private fun Response.into(response: org.apache.http.HttpResponse) {
        with(response) {
            setStatusCode(status.code)
            setReasonPhrase(status.description)
            headers.forEach { (key, value) -> addHeader(key, value) }
            entity = InputStreamEntity(body.stream)
        }
    }

    private fun Array<Header>.toHttp4kHeaders(): Headers = listOf(*this.map { it.name to it.value }.toTypedArray())
}

data class ApacheServer(val port: Int = 8000) : ServerConfig {
    override fun toServer(httpHandler: HttpHandler): Http4kServer = object:Http4kServer {

        val server = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setSocketConfig(SocketConfig.custom()
                        .setTcpNoDelay(true)
                        .setSoKeepAlive(true)
                        .setSoReuseAddress(true)
                        .setBacklogSize(128)
                        .build())
                .registerHandler("*", Http4kRequestHandler(httpHandler))
                .create()

        override fun start(): Http4kServer = apply {
            server.start()
        }

        override fun stop() {
            server.shutdown(15, TimeUnit.SECONDS)
        }

    }
}