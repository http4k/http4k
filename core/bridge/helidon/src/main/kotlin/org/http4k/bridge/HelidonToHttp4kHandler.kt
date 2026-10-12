package org.http4k.bridge

import io.helidon.http.Headers
import io.helidon.http.Status.create
import io.helidon.webserver.http.Handler
import io.helidon.webserver.http.ServerRequest
import io.helidon.webserver.http.ServerResponse
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Uri
import org.http4k.server.supportedOrNull
import org.http4k.sse.SseHandler
import org.http4k.sse.SseResponse
import org.http4k.sse.encodingHeaders
import org.http4k.sse.streamHeaders
import org.http4k.sse.streamTo

fun HelidonToHttp4kHandler(http: HttpHandler?, sse: SseHandler?) = Handler { req, res ->
    val httpToUse = http ?: { Response(NOT_FOUND) }
    req.toHttp4k()
        ?.let {
            when {
                sse != null && it.isEventStream() -> {
                    val http4kResponse = sse(it)
                    when {
                        http4kResponse.handled -> http4kResponse.writeInto(it, res)
                        else -> res.from(httpToUse(it))
                    }
                }

                else -> res.from(httpToUse(it))
            }
        }
        ?: res.from(Response(NOT_IMPLEMENTED))
}

private fun SseResponse.writeInto(http4kRequest: Request, res: ServerResponse) {
    (headers + encodingHeaders).groupBy { it.first }.forEach {
        res.header(it.key, *it.value.map { it.second ?: "" }.toTypedArray<String>())
    }
    res.status(create(status.code, status.description))

    when {
        status.code < 400 -> {
            streamHeaders.forEach { (name, value) -> res.header(name, value) }
            res.outputStream().use { streamTo(http4kRequest, it) }
        }

        else -> res.send()
    }
}

private fun Request.isEventStream() =
    headerValues("Accept").any { it?.contains(TEXT_EVENT_STREAM.value, true) == true }

private fun ServerRequest.toHttp4k(): Request? =
    Method.supportedOrNull(prologue().method().text())
        ?.let {
            Request(it, Uri.of(path().rawPath() + query()), prologue().rawProtocol())
                .body(content().inputStream(), headers().contentLength().let {
                    if (it.isPresent) it.asLong else null
                })
                .headers(headers())
                .source(RequestSource(remotePeer().host(), remotePeer().port()))
        }

private fun ServerResponse.from(response: Response) = apply {
    status(create(response.status.code, response.status.description))
    response.headers.groupBy { it.first }.forEach {
        this.header(it.key, *it.value.map { it.second ?: "" }.toTypedArray<String>())
    }
    outputStream().use { response.body.stream.copyTo(it) }
}

internal fun Request.headers(headers: Headers) =
    headers(headers.flatMap { value -> value.allValues().map { value.headerName().defaultCase() to it } })
