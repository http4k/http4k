package org.http4k.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.withContext
import org.http4k.core.Headers
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.lens.Header
import org.http4k.server.ServerConfig.StopMode.Immediate
import java.util.concurrent.TimeUnit.SECONDS
import io.ktor.http.Headers as KHeaders

@Suppress("EXPERIMENTAL_API_USAGE")
class KtorCIO(val port: Int = 8000, override val stopMode: ServerConfig.StopMode) : ServerConfig {
    constructor(port: Int = 8000) : this(port, Immediate)

    init {
        if (stopMode != Immediate) {
            throw ServerConfig.UnsupportedStopMode(stopMode)
        }
    }

    override fun toServer(http: HttpHandler): Http4kServer = object : Http4kServer {
        private val engine: CIOApplicationEngine = embeddedServer(CIO, port) {
            install(createApplicationPlugin(name = "http4k") {
                onCall {
                    withContext(Default) {
                        it.response.fromHttp4K(http(it.request.asHttp4k()))
                    }
                }
            })
        }

        override fun start() = apply {
            engine.start()
        }

        override fun stop() = apply {
            engine.stop(0, 2, SECONDS)
        }

        override fun port() = engine.environment.connectors[0].port
    }
}

fun ApplicationRequest.asHttp4k() = Request(Method.valueOf(httpMethod.value), uri)
    .headers(headers.toHttp4kHeaders())
    .body(receiveChannel().toInputStream(), header("Content-Length")?.toLong())
    .source(RequestSource(origin.remoteHost, scheme = origin.scheme)) // origin.remotePort does not exist for Ktor

suspend fun ApplicationResponse.fromHttp4K(response: Response) {
    status(HttpStatusCode.fromValue(response.status.code))
    response.headers
        .filterNot { HttpHeaders.isUnsafe(it.first) }
        .forEach { header(it.first, it.second ?: "") }
    call.respondOutputStream(
        Header.CONTENT_TYPE(response)?.let { ContentType.parse(it.toHeaderValue()) }
    ) {
        response.body.stream.copyTo(this)
    }
}

private fun KHeaders.toHttp4kHeaders(): Headers = names().flatMap { name ->
    (getAll(name) ?: emptyList()).map { name to it }
}
