package org.http4k.server

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.sse.ServerSentEventConnection
import io.undertow.server.handlers.sse.ServerSentEventHandler
import io.undertow.util.HttpString
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Uri
import org.http4k.core.toParametersMap
import org.http4k.sse.PushAdaptingSse
import org.http4k.sse.SseHandler
import org.http4k.sse.SseMessage
import java.io.IOException

class Http4kSetHeadersHandler(private val sse: SseHandler) : HttpHandler {

    override fun handleRequest(exchange: HttpServerExchange) {
        val (status, headers, consumer) = sse(exchange.asRequest() ?: error("Cannot create request from exchange"))
        exchange.setStatusCode(status.code)

        headers.toParametersMap().forEach { (name, values) ->
            exchange.responseHeaders.putAll(HttpString(name), values.toList())
        }

        val next = ServerSentEventHandler { connection, _ ->
            val socket = object : PushAdaptingSse() {
                override fun send(message: SseMessage) =
                    when (message) {
                        is SseMessage.Retry -> connection.sendRetry(message.backoff.toMillis())
                        is SseMessage.Data -> connection.send(message.data)
                        is SseMessage.Event -> connection.send(message.data, message.event, message.id, NoOp)
                    }

                override fun close() = connection.close()
            }

            consumer(socket)

            connection.addCloseTask { socket.triggerClose() }
        }

        next.handleRequest(exchange)
    }

    private fun HttpServerExchange.asRequest() =
        Method.supportedOrNull(requestMethod.toString())?.let {
            Request(it, Uri.of("$relativePath?$queryString"))
                .headers(requestHeaders
                    .flatMap { header -> header.map { header.headerName.toString() to it } })
                .source(RequestSource(sourceAddress.hostString, sourceAddress.port, requestScheme))
        }
}

private object NoOp : ServerSentEventConnection.EventCallback {
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
    it.requestHeaders["Accept"]?.any { it.equals(ContentType.TEXT_EVENT_STREAM.value, true) } ?: false
}
