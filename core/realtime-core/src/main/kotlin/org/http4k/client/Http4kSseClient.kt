package org.http4k.client

import org.http4k.client.SseReconnectionMode.Disconnect
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.LAST_EVENT_ID
import org.http4k.lens.accept
import org.http4k.sse.SseEventId
import org.http4k.sse.SseMessage
import org.http4k.testing.SseClient
import java.io.InputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * Simple SSE client leveraging standard HttpHandlers. Tracks a single connection only and sends reconnection
 * requests including the last event id.
 */
class Http4kSseClient(
    private val sseRequest: Request,
    private val http: HttpHandler,
    private var reconnectionMode: SseReconnectionMode = Disconnect,
    private val reportError: (Exception) -> Unit = {}
) : SseClient {

    private val running = AtomicBoolean(true)
    private val messageQueue = LinkedBlockingQueue<SseMessage>()

    private val lastEventId = AtomicReference<SseEventId>()

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
            if (value is SseMessage.Event) lastEventId.set(value.id)
        }
    }

    override fun close() {
        running.set(false)
        reconnectionMode = Disconnect
    }
}

fun InputStream.chunkedSseSequence(): Sequence<SseMessage> = sequence {
    use {
        val buffer = StringBuilder()
        var lastChar: Int = -1
        var newlineCount = 0

        while (true) {
            val current = it.read()
            if (current == -1) {
                if (buffer.isNotEmpty()) {
                    try {
                        yield(SseMessage.parse(buffer.toString()))
                    } catch (e: Exception) {
                        buffer.clear()
                    }
                }
                break
            }

            val currentChar = current.toChar()
            buffer.append(currentChar)

            if (currentChar == '\n') {
                if (lastChar == '\n'.code) {
                    newlineCount++
                    if (newlineCount == 2) {
                        try {
                            yield(SseMessage.parse(buffer.substring(0, buffer.length - 2)))
                        } catch (e: Exception) {
                        }
                        buffer.clear()
                        newlineCount = 0
                    }
                } else {
                    newlineCount = 1
                }
            } else {
                newlineCount = 0
            }

            lastChar = current
        }
    }
}
