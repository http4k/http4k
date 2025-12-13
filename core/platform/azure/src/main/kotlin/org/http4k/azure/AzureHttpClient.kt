package org.http4k.azure

import com.azure.core.http.HttpClient
import com.azure.core.http.HttpHeader
import com.azure.core.http.HttpHeaders
import com.azure.core.http.HttpRequest
import com.azure.core.http.HttpResponse
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers.boundedElastic
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * Pluggable Http client adapter for Azure SDK.
 */
class AzureHttpClient(private val http: HttpHandler) : HttpClient {
    override fun send(request: HttpRequest) = http(request.toHttp4k()).fromHttp4k(request)
}

internal fun Flux<ByteBuffer>.toInputStream(): InputStream {
    val pipedInputStream = PipedInputStream()
    val pipedOutputStream = PipedOutputStream(pipedInputStream)

    subscribeOn(boundedElastic())
        .doOnNext { pipedOutputStream.write(ByteArray(it.remaining()).apply { it.get(this) }) }
        .doFinally { runCatching { pipedOutputStream.close() } }
        .subscribe()

    return pipedInputStream
}

private fun Response.fromHttp4k(request: HttpRequest): Mono<HttpResponse> = Mono.just(
    object : HttpResponse(request) {
        override fun getStatusCode() = this@fromHttp4k.status.code

        @Deprecated("Deprecated in Java")
        override fun getHeaderValue(p0: String) = this@fromHttp4k.header(p0) ?: ""

        override fun getHeaders() = HttpHeaders(
            this@fromHttp4k.headers.groupBy { it.first }.map { (k, v) -> HttpHeader(k, v.map { it.second ?: "" }) }
        )

        override fun getBody() = this@fromHttp4k.body.stream.inputStreamToFlux()

        fun InputStream.inputStreamToFlux() = Flux.generate {
            val buffer = ByteArray(4096)
            when (val bytesRead = read(buffer)) {
                -1 -> it.complete()
                else -> it.next(ByteBuffer.wrap(buffer, 0, bytesRead))
            }
        }.doFinally { close() }

        override fun getBodyAsByteArray() = Mono.just(this@fromHttp4k.body.stream.readBytes())

        override fun getBodyAsString() = Mono.just(this@fromHttp4k.bodyString())

        override fun getBodyAsString(p0: Charset) = Mono.just(this@fromHttp4k.body.stream.reader(p0).readText())
    })

private fun HttpRequest.toHttp4k() = Request(Method.valueOf(httpMethod.name), url.toExternalForm())
    .headers(headers.flatMap { h -> h.values.map { h.name to it } })
    .body(body.toInputStream())
