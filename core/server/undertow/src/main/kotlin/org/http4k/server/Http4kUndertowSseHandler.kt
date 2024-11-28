package org.http4k.server

import io.undertow.server.handlers.sse.ServerSentEventConnection
import io.undertow.server.handlers.sse.ServerSentEventConnection.EventCallback
import io.undertow.server.handlers.sse.ServerSentEventHandler
import org.http4k.core.Request
import org.http4k.sse.PushAdaptingSse
import org.http4k.sse.SseConsumer
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Data
import org.http4k.sse.SseMessage.Event
import org.http4k.sse.SseMessage.Retry
import java.io.IOException
import java.nio.channels.ClosedChannelException

fun Http4kUndertowSseHandler(request: Request, consumer: SseConsumer) =
    ServerSentEventHandler { connection, _ ->
        val socket = object : PushAdaptingSse(request) {

            override fun send(message: SseMessage) = apply {
                when (message) {
                    is Retry -> connection.sendRetry(message.backoff.toMillis(), CloseOnFailure)
                    is Data -> connection.send(message.data, CloseOnFailure)
                    is Event -> connection.send(message.data, message.event, message.id, CloseOnFailure)
                }
            }

            override fun close() = connection.shutdown()
        }

        connection.addCloseTask { socket.triggerClose() }

        consumer(socket)
    }

private object CloseOnFailure : EventCallback {
    override fun done(
        connection: ServerSentEventConnection?,
        data: String?,
        event: String?,
        id: String?
    ) {
    }

    override fun failed(
        connection: ServerSentEventConnection,
        data: String?,
        event: String?,
        id: String?,
        e: IOException?
    ) {
        if (e !is ClosedChannelException) e?.printStackTrace()
        connection.close()
    }
}
