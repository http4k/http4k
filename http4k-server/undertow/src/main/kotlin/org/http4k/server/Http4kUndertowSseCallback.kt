package org.http4k.server

import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.sse.ServerSentEventConnection
import io.undertow.server.handlers.sse.ServerSentEventConnection.EventCallback
import io.undertow.server.handlers.sse.ServerSentEventConnectionCallback
import io.undertow.util.HttpString
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.sse.PushAdaptingSse
import org.http4k.sse.SseHandler
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Data
import org.http4k.sse.SseMessage.Event
import org.http4k.sse.SseMessage.Retry
import java.io.IOException

class Http4kSseCallback(private val sseHandler: SseHandler) : ServerSentEventConnectionCallback {

    override fun connected(connection: ServerSentEventConnection, lastEventId: String?) {
        val connectRequest = connection.asRequest()

        val socket = object : PushAdaptingSse() {
            override fun send(message: SseMessage) =
                when (message) {
                    is Retry -> connection.sendRetry(message.backoff.toMillis())
                    is Data -> connection.send(message.data)
                    is Event -> connection.send(message.data, message.event, message.id, NoOp)
                }

            override fun close() = connection.close()
        }
        val sse = sseHandler(connectRequest)
        val (_, _, consumer) = sse

        consumer(socket)

        connection.addCloseTask { socket.triggerClose() }
    }

    private fun ServerSentEventConnection.asRequest(): Request =
        Request(Method.GET, Uri.of("$requestURI?$queryString"))
            .headers(requestHeaders
                .flatMap { header -> header.map { header.headerName.toString() to it } })
}

private object NoOp : EventCallback {
    override fun done(
        connection: ServerSentEventConnection?,
        data: String?,
        event: String?,
        id: String?
    ) {
    }

    override fun failed(
        connection: ServerSentEventConnection?,
        data: String?,
        event: String?,
        id: String?,
        e: IOException?
    ) {
        e?.printStackTrace()
    }
}

fun hasEventStreamContentType(): (HttpServerExchange) -> Boolean = {
    it.requestHeaders["Accept"]?.any { it.equals(TEXT_EVENT_STREAM.value, true) } ?: false
}
