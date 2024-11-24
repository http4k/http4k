package org.http4k.server

import io.helidon.http.Status.create
import io.helidon.http.sse.SseEvent.builder
import io.helidon.webserver.http.Handler
import io.helidon.webserver.http.ServerRequest
import io.helidon.webserver.http.ServerResponse
import io.helidon.webserver.sse.SseSink
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri
import org.http4k.lens.contentType
import org.http4k.sse.PushAdaptingSse
import org.http4k.sse.SseHandler
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Data
import org.http4k.sse.SseMessage.Event
import org.http4k.sse.SseMessage.Retry

fun HelidonHandler(http: HttpHandler?, sse: SseHandler?) = Handler { req, res ->
    res.from(req.toHttp4k()
        ?.let { http4kReq ->
            when {
                sse != null && http4kReq.isEventStream() -> sse.handle(http4kReq, res)
                else -> http?.let { it(http4kReq) }
            }
        }
        ?: Response(NOT_FOUND))
}

private fun SseHandler.handle(http4kRequest: Request, res: ServerResponse): Response {
    val http4kResponse = this(http4kRequest)

    http4kResponse.consumer(object : PushAdaptingSse(http4kRequest) {
        private val sseSink = res.sink(SseSink.TYPE)
        override fun send(message: SseMessage) {
            sseSink.emit(
                when (message) {
                    is Retry -> builder().reconnectDelay(message.backoff).build()
                    is Data -> builder().data(message.data).build()
                    is Event -> builder().name(message.event).build()
                }
            )
        }

        override fun close() = sseSink.close()
    })

    return Response(http4kResponse.status)
        .contentType(TEXT_EVENT_STREAM)
        .headers(http4kResponse.headers)
}

private fun Request.isEventStream() =
    headerValues("Accept").any { it?.contains(TEXT_EVENT_STREAM.value) == true }

private fun ServerRequest.toHttp4k(): Request? =
    Method.supportedOrNull(prologue().method().text())
        ?.let {
            Request(it, Uri.of(path().rawPath() + query()), prologue().rawProtocol())
                .body(content().inputStream(), headers().contentLength().let {
                    if (it.isPresent) it.asLong else null
                })
                .headers(
                    headers()
                        .flatMap { value -> value.allValues().map { value.headerName().defaultCase() to it } }
                )
                .source(RequestSource(remotePeer().host(), remotePeer().port()))
        }

private fun ServerResponse.from(response: Response) = apply {
    status(create(response.status.code, response.status.description))
    response.headers.groupBy { it.first }.forEach {
        header(it.key, *it.value.map { it.second ?: "" }.toTypedArray())
    }
    outputStream().use { response.body.stream.copyTo(it) }
}
