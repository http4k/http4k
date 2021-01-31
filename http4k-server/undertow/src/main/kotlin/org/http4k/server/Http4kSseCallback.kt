package org.http4k.server

import io.undertow.server.handlers.sse.ServerSentEventConnection
import io.undertow.server.handlers.sse.ServerSentEventConnectionCallback
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.sse.PushAdaptingSse
import org.http4k.sse.SseHandler
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.*
import java.io.IOException

class Http4kSseCallback(private val sse: SseHandler) : ServerSentEventConnectionCallback {

    override fun connected(connection: ServerSentEventConnection, lastEventId: String?) {
        val connectRequest = connection.asRequest()

        sse(connectRequest)?.also {
            val socket = object : PushAdaptingSse(connectRequest) {
                override fun send(message: SseMessage) =
                    when (message) {
                        is Retry -> connection.sendRetry(message.backoff.toMillis())
                        is Data -> connection.send(message.data)
                        is Event -> connection.send(message.data, message.event, message.id, NoOp)
                    }

                override fun close() = connection.close()
            }.apply(it)

            connection.addCloseTask {
                socket.triggerClose()
            }
        } ?: connection.close()
    }

    private fun ServerSentEventConnection.asRequest(): Request =
        Request(Method.GET, Uri.of("$requestURI?$queryString"))
            .headers(requestHeaders
                .flatMap { header -> header.map { header.headerName.toString() to it } })
}

object NoOp : ServerSentEventConnection.EventCallback {
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
    }
}
