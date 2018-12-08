package org.http4k.server

import io.ktor.application.ApplicationCallPipeline
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import io.ktor.request.httpMethod
import io.ktor.request.uri
import io.ktor.response.ApplicationResponse
import io.ktor.response.header
import io.ktor.response.respondBytes
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.util.toMap
import kotlinx.coroutines.io.jvm.javaio.toInputStream
import org.http4k.client.JavaHttpClient
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
                val response = httpHandler(context.request.asHttp4k())
                println(response)
                response.transfer(context.response)
                context.respondBytes(
                    contentType = Header.CONTENT_TYPE(response)?.let { ContentType.parse(it.toHeaderValue()) },
                    provider = { response.body.payload.array() }

                )
            }
        }

        override fun start() = apply { engine.start() }

        override fun stop() = apply {
            engine.stop(1, 1, SECONDS)
        }

        override fun port() = engine.environment.connectors[0].port
    }
}

private fun KHeaders.toHttp4kHeaders() = toMap().flatMap { it.value.map { value -> it.key to value } }

private fun ApplicationRequest.asHttp4k() =
    Request(Method.valueOf(httpMethod.value), uri)
        .body(receiveChannel().toInputStream())
        .headers(headers.toHttp4kHeaders())

private fun Response.transfer(ktor: ApplicationResponse) {
    ktor.status(HttpStatusCode.fromValue(status.code))
    headers
        .filterNot { HttpHeaders.isUnsafe(it.first) }
        .forEach { (key, value) -> ktor.header(key, value ?: "") }
}

fun main(args: Array<String>) {
    { r: Request ->
        Response(OK).body(r.body).header("foo", "bar").header("foo", "bar2")
    }
        .asServer(KtorCIO(9000)).start().use {
            println(JavaHttpClient()(Request(POST, "http://localhost:9000/foo").body("hello")))
        }
}