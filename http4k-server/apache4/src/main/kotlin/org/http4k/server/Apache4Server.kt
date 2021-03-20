package org.http4k.server

import org.apache.http.Header
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.HttpInetConnection
import org.apache.http.config.SocketConfig
import org.apache.http.entity.InputStreamEntity
import org.apache.http.impl.bootstrap.HttpServer
import org.apache.http.impl.bootstrap.ServerBootstrap
import org.apache.http.impl.io.EmptyInputStream
import org.apache.http.protocol.HttpContext
import org.apache.http.protocol.HttpCoreContext
import org.apache.http.protocol.HttpRequestHandler
import org.http4k.core.Headers
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.safeLong
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import java.net.InetAddress
import java.util.concurrent.TimeUnit.SECONDS
import org.apache.http.HttpRequest as ApacheRequest
import org.apache.http.HttpResponse as ApacheResponse

/**
 * Exposed to allow for insertion into a customised Apache WebServer instance
 */
class Http4kApache4RequestHandler(handler: HttpHandler) : HttpRequestHandler {

    private val safeHandler = ServerFilters.CatchAll().then(handler)

    override fun handle(request: ApacheRequest, response: ApacheResponse, context: HttpContext) {
        safeHandler(request.asHttp4kRequest(context)).into(response)
    }

    private fun ApacheRequest.asHttp4kRequest(context: HttpContext): Request {
        val connection = context.getAttribute(HttpCoreContext.HTTP_CONNECTION) as HttpInetConnection
        return Request(Method.valueOf(requestLine.method), requestLine.uri)
            .headers(allHeaders.toHttp4kHeaders()).let {
                when (this) {
                    is HttpEntityEnclosingRequest -> it.body(entity.content, getFirstHeader("Content-Length")?.value.safeLong())
                    else -> it.body(EmptyInputStream.INSTANCE, 0)
                }
            }
            .source(RequestSource(connection.remoteAddress.hostAddress, connection.remotePort))
    }

    private val headersThatApacheInterceptorSets = setOf("Transfer-Encoding", "Content-Length")

    private fun Response.into(response: ApacheResponse) {
        with(response) {
            setStatusCode(status.code)
            setReasonPhrase(status.description)
            headers.filter { !headersThatApacheInterceptorSets.contains(it.first) }.forEach { (key, value) -> addHeader(key, value) }
            entity = InputStreamEntity(body.stream, body.length ?: -1L)
        }
    }

    private fun Array<Header>.toHttp4kHeaders(): Headers = listOf(*map { it.name to it.value }.toTypedArray())
}

class Apache4Server(val port: Int = 8000, val address: InetAddress?) : ServerConfig {
    constructor(port: Int = 8000) : this(port, null)

    override fun toServer(http: HttpHandler): Http4kServer = object : Http4kServer {
        private val server: HttpServer

        init {
            val bootstrap = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setSocketConfig(SocketConfig.custom()
                    .setTcpNoDelay(true)
                    .setSoKeepAlive(true)
                    .setSoReuseAddress(true)
                    .setBacklogSize(1000)
                    .build())
                .registerHandler("*", Http4kApache4RequestHandler(http))

            if (address != null)
                bootstrap.setLocalAddress(address)

            server = bootstrap.create()
        }

        override fun start() = apply { server.start() }

        override fun stop() = apply { server.shutdown(1, SECONDS) }

        override fun port(): Int = if (port != 0) port else server.localPort
    }
}
