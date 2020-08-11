package org.http4k.server

import io.ktor.application.ApplicationCallPipeline.ApplicationPhase.Call
import io.ktor.features.origin
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import io.ktor.request.httpMethod
import io.ktor.request.uri
import io.ktor.response.ApplicationResponse
import io.ktor.response.header
import io.ktor.response.respondOutputStream
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EngineAPI
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.utils.io.jvm.javaio.toInputStream
import org.http4k.core.Headers
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.lens.Header
import org.http4k.server.ServerConfig.StopMode
import java.time.Duration
import java.util.concurrent.TimeUnit.MILLISECONDS
import io.ktor.http.Headers as KHeaders

@Suppress("EXPERIMENTAL_API_USAGE")
class KtorCIO(val port: Int = 8000, override val stopMode: StopMode) : ServerConfig {
    constructor(port: Int = 8000) : this(8000, StopMode.Delayed(Duration.ofSeconds(2)))

    @OptIn(EngineAPI::class)
    override fun toServer(http: HttpHandler): Http4kServer = object : Http4kServer {
        private val engine: CIOApplicationEngine = embeddedServer(CIO, port) {
            intercept(Call) {
                with(context) { response.fromHttp4K(http(request.asHttp4k())) }
                return@intercept finish()
            }
        }

        override fun start() = apply {
            engine.start()
        }

        override fun stop() = apply {
            val (gracePeriodMillis, timeoutMillis) = when(stopMode) {
                is StopMode.Graceful -> 1000L to stopMode.timeout.toMillis()
                is StopMode.Immediate -> 0L to 0L
                is StopMode.Delayed -> stopMode.timeout.toMillis() to stopMode.timeout.toMillis()
            }
            engine.stop(gracePeriodMillis, timeoutMillis, MILLISECONDS)
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
