package org.http4k.bridge

import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.*
import io.ktor.server.plugins.origin
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.httpVersion
import io.ktor.server.request.uri
import io.ktor.server.response.ApplicationResponse
import io.ktor.server.response.header
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.toInputStream
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.server.supportedOrNull
import java.io.InputStream
import java.io.OutputStream

@OptIn(InternalAPI::class)
fun KtorToHttp4kApplicationPlugin(http: HttpHandler) = createApplicationPlugin(name = "http4k") {
    val routingKey: AttributeKey<HttpStatusCode> = RoutingFailureStatusCode
    val handledByHttp4kKey: AttributeKey<Unit> = AttributeKey("HandledByHttp4k")

    on(ResponseBodyReadyForSend) { call, _ ->
        if (!call.attributes.contains(handledByHttp4kKey)
            && call.attributes.contains(routingKey) && call.attributes[routingKey] == NotFound
        ) {
            call.attributes.put(handledByHttp4kKey, Unit)
            call.response.fromHttp4K(call.request.asHttp4k()?.let(http) ?: Response(Status.NOT_IMPLEMENTED))
        }
    }
}

fun ApplicationRequest.asHttp4k() = Method.supportedOrNull(httpMethod.value)?.let {
    Request(it, uri, httpVersion)
        .headers(headers.toHttp4kHeaders())
        .body(receiveChannel().toInputStream(), header("Content-Length")?.toLong())
        .source(RequestSource(origin.remoteHost, scheme = origin.scheme)) // origin.remotePort does not exist for Ktor
}

suspend fun ApplicationResponse.fromHttp4K(response: Response) {
    status(HttpStatusCode.fromValue(response.status.code))
    response.headers
        .filterNot { HttpHeaders.isUnsafe(it.first) || it.first == CONTENT_TYPE.meta.name }
        .forEach { header(it.first, it.second ?: "") }
    call.respondOutputStream(
        CONTENT_TYPE(response)?.let { ContentType.parse(it.toHeaderValue()) }
    ) { response.body.stream.copyFlushingTo(this) }
}

private fun InputStream.copyFlushingTo(outputStream: OutputStream) {
    var bytesCopied: Long = 0
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var bytes = read(buffer)
    while (bytes >= 0) {
        outputStream.write(buffer, 0, bytes)
        outputStream.flush() // flush each buffer to ensure data is written immediately
        bytesCopied += bytes
        bytes = read(buffer)
    }
}

private fun Headers.toHttp4kHeaders() = names().flatMap { name ->
    (getAll(name) ?: emptyList()).map { name to it }
}
