package org.http4k.server

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.origin
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.ApplicationResponse
import io.ktor.server.response.header
import io.ktor.server.response.respondOutputStream
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.withContext
import org.http4k.core.Headers
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.server.ServerConfig.StopMode.Graceful
import org.http4k.server.ServerConfig.StopMode.Immediate
import java.util.concurrent.TimeUnit.MILLISECONDS
import io.ktor.http.Headers as KHeaders

@Suppress("EXPERIMENTAL_API_USAGE")
class KtorNetty(val port: Int = 8000, override val stopMode: ServerConfig.StopMode) : ServerConfig {
    constructor(port: Int = 8000) : this(port, Immediate)

    override fun toServer(http: HttpHandler): Http4kServer = object : Http4kServer {
        private val engine = embeddedServer(Netty, port) {
            install(createApplicationPlugin(name = "http4k") {
                onCall {
                    withContext(Default) {
                        it.response.fromHttp4K(it.request.asHttp4k()?.let(http) ?: Response(NOT_IMPLEMENTED))
                    }
                }
            })
        }

        override fun start() = apply {
            engine.start()
        }

        override fun stop() = apply {
            when (stopMode) {
                is Immediate -> engine.stop(0, 0, MILLISECONDS)
                is Graceful -> engine.stop(stopMode.timeout.toMillis(), stopMode.timeout.toMillis(), MILLISECONDS)
            }
        }

        override fun port() = engine.environment.connectors[0].port
    }
}

fun ApplicationRequest.asHttp4k() = runCatching {
    Request(Method.valueOf(httpMethod.value), uri)
        .headers(headers.toHttp4kHeaders())
        .body(receiveChannel().toInputStream(), header("Content-Length")?.toLong())
        .source(RequestSource(origin.remoteHost)) // origin.remotePort does not exist for Ktor
}.getOrNull()

suspend fun ApplicationResponse.fromHttp4K(response: Response) {
    status(HttpStatusCode.fromValue(response.status.code))
    response.headers
        .filterNot { HttpHeaders.isUnsafe(it.first) || it.first == CONTENT_TYPE.meta.name }
        .forEach { header(it.first, it.second ?: "") }
    call.respondOutputStream(
        CONTENT_TYPE(response)?.let { ContentType.parse(it.toHeaderValue()) }
    ) {
        response.body.stream.copyTo(this)
    }
}

private fun KHeaders.toHttp4kHeaders(): Headers = names().flatMap { name ->
    (getAll(name) ?: emptyList()).map { name to it }
}
