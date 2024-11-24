package org.http4k.server

import io.helidon.http.Status.create
import io.helidon.http.sse.SseEvent.builder
import io.helidon.webserver.http.Handler
import io.helidon.webserver.http.ServerRequest
import io.helidon.webserver.http.ServerResponse
import io.helidon.webserver.sse.SseSink.TYPE
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Uri
import org.http4k.sse.PushAdaptingSse
import org.http4k.sse.SseHandler
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Data
import org.http4k.sse.SseMessage.Event
import org.http4k.sse.SseMessage.Retry

fun HelidonHandler(http: HttpHandler?, sse: SseHandler?) = Handler { req, res ->
    req.toHttp4k()
        ?.let { http4kReq ->
            when {
                sse != null && http4kReq.isEventStream() -> sse.handle(http4kReq, res)
                else -> http?.let { res.from(it(http4kReq)) }
            }
        }
        ?: res.from(Response(NOT_IMPLEMENTED))
}

private fun SseHandler.handle(http4kRequest: Request, res: ServerResponse) {
    val http4kResponse = this(http4kRequest)

    http4kResponse.headers.groupBy { it.first }.forEach {
        res.header(it.key, *it.value.map { it.second ?: "" }.toTypedArray<String>())
    }

    val sseSink = res.sink(TYPE)

    res.status(create(http4kResponse.status.code, http4kResponse.status.description))

    http4kResponse.consumer(object : PushAdaptingSse(http4kRequest) {
        override fun send(message: SseMessage) {
            sseSink.emit(
                when (message) {
                    is Retry -> builder().reconnectDelay(message.backoff).build()
                    is Data -> builder().data(message.sanitizeForMultipleRecords()).build()
                    is Event -> builder().name(message.event).data(message.data.replace("\n", "\ndata:")).id(message.id).build()
                }
            )
        }

        private fun Data.sanitizeForMultipleRecords() = data.replace("\n", "\ndata:")

        override fun close() {
            sseSink.close()
        }
    })
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
        this.header(it.key, *it.value.map { it.second ?: "" }.toTypedArray<String>())
    }
    outputStream().use { response.body.stream.copyTo(it) }
}
