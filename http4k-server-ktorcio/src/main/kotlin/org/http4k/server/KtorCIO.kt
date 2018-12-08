package org.http4k.server

import io.ktor.application.ApplicationCallPipeline
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
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.io.jvm.javaio.toInputStream
import org.http4k.client.JavaHttpClient
import org.http4k.core.Headers
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Header
import java.util.concurrent.TimeUnit.SECONDS
import io.ktor.http.Headers as KHeaders

@Suppress("EXPERIMENTAL_API_USAGE")
data class KtorCIO(val port: Int = 8000) : ServerConfig {

    override fun toServer(httpHandler: HttpHandler): Http4kServer = object : Http4kServer {
        private val engine = embeddedServer(CIO, port) {
            intercept(ApplicationCallPipeline.Call) {
                context.response.fromHttp4K(httpHandler(context.request.asHttp4k()))
            }
        }

        override fun start() = apply { engine.start() }

        override fun stop() = apply { engine.stop(1, 1, SECONDS) }

        override fun port() = engine.environment.connectors[0].port
    }
}

private fun KHeaders.toHttp4kHeaders(): Headers = names().flatMap { name ->
    (getAll(name) ?: emptyList()).map { name to it }
}

fun ApplicationRequest.asHttp4k() = Request(Method.valueOf(httpMethod.value), uri)
    .headers(headers.toHttp4kHeaders())
    .body(receiveChannel().toInputStream(), header("Content-Length")?.toLong())

private suspend fun ApplicationResponse.fromHttp4K(response: Response) {
    status(HttpStatusCode.fromValue(response.status.code))
    response.headers
        .filterNot { HttpHeaders.isUnsafe(it.first) }
        .forEach { (key, value) -> header(key, value ?: "") }
    call.respondOutputStream(
        contentType = Header.CONTENT_TYPE(response)?.let { ContentType.parse(it.toHeaderValue()) }
    ) { response.body.stream.copyTo(this) }
}

fun main(args: Array<String>) {
    { r: Request ->
        Response(OK).body(r.body).header("foo", "bar").header("foo", "bar2")
    }
        .asServer(KtorCIO(9000)).start().use {
            println(JavaHttpClient()(Request(POST, "http://localhost:9000/foo").body("hello")))
        }
}