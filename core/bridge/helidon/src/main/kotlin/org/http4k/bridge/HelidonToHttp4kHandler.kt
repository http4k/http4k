package org.http4k.bridge

import io.helidon.http.Headers
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
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Uri
import org.http4k.server.supportedOrNull
import org.http4k.sse.PushAdaptingSse
import org.http4k.sse.SseHandler
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Data
import org.http4k.sse.SseMessage.Event
import org.http4k.sse.SseMessage.Ping
import org.http4k.sse.SseMessage.Retry
import org.http4k.sse.SseResponse
import java.util.concurrent.CountDownLatch

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

    headers.groupBy { it.first }.forEach {
        res.header(it.key, *it.value.map { it.second ?: "" }.toTypedArray<String>())
    }

    when {
        status.code < 400 -> {
            val sseSink = res.sink(TYPE)

            res.status(create(status.code, status.description))

            val latch = CountDownLatch(1)

            val sse = object : PushAdaptingSse(http4kRequest) {
                override fun send(message: SseMessage) = apply {
                    try {
                        sseSink.emit(
                            when (message) {
                                is Retry -> builder().reconnectDelay(message.backoff).data("")
                                is Ping -> builder().data("")
                                is Data -> builder().data(message.sanitizeForMultipleRecords())
                                is Event -> builder().name(message.event).data(message.data.replace("\n", "\ndata:"))
                                    .let { if (message.id == null) it else it.id(message.id?.value) }
                            }.build()
                        )
                    } catch (e: Exception) {
                        triggerClose()
                        latch.countDown()
                    }
                }

                private fun Data.sanitizeForMultipleRecords() = data.replace("\n", "\ndata:")

                override fun close() {
                    try {
                        sseSink.close()
                    } finally {
                        triggerClose()
                        latch.countDown()
                    }
                }
            }

            sse.onClose(latch::countDown)

            try {
                consumer(sse)
            } catch (e: Exception) {
                sse.close()
            }

            latch.await()
        }
        else -> res.status(create(status.code, status.description)).send()
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
