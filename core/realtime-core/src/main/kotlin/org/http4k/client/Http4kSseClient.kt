package org.http4k.client

import org.http4k.client.ReconnectionMode.Disconnect
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.LAST_EVENT_ID
import org.http4k.lens.accept
import org.http4k.sse.SseClient
import org.http4k.sse.SseEventId
import org.http4k.sse.SseMessage
import org.http4k.sse.chunkedSseSequence
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * Simple SSE client leveraging standard HttpHandlers. Tracks a single connection only and sends reconnection
 * requests including the last event id.
 *
 *  Note that the representation uses an unbounded blocking queue, so clients are required to consume messages
 *  using received().
 */
class Http4kSseClient(
    private val sseRequest: Request,
    private val http: HttpHandler,
    private var reconnectionMode: ReconnectionMode = Disconnect,
    private val reportError: (Exception) -> Unit = {},
) : SseClient {
    private val running = AtomicBoolean(true)

    private val lastEventId = AtomicReference<SseEventId>()

    private val messageQueue: BlockingQueue<SseMessage> = LinkedBlockingQueue()

    override fun received(): Sequence<SseMessage> = sequence {
        thread {
            do {
                try {
                    val response = http(
                        sseRequest
                            .with(Header.LAST_EVENT_ID of lastEventId.get())
                            .accept(TEXT_EVENT_STREAM)
                    )

                    when {
                        response.status.successful ->
                            response.body.stream.chunkedSseSequence().forEach(messageQueue::put)

                        else -> error("Failed to connect to ${sseRequest.uri} ${response.status}")
                    }
                } catch (e: Exception) {
                    reportError(e)
                }
            } while (reconnectionMode.doReconnect())
        }

        while (running.get()) {
            val value = messageQueue.take()
            yield(value)
            if (value is SseMessage.Event && value.id != null) lastEventId.set(value.id)
        }
    }

    override fun close() {
        running.set(false)
        reconnectionMode = Disconnect
    }
}
