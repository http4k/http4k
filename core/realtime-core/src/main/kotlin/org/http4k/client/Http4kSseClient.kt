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
import java.io.InputStream
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

fun InputStream.chunkedSseSequence(): Sequence<SseMessage> = sequence {
    use {
        val buffer = StringBuilder()
        var consecutiveLineBreaks = 0
        var lastWasCR = false

        while (true) {
            val current = it.read()
            if (current == -1) {
                if (buffer.isNotEmpty()) {
                    try {
                        yield(SseMessage.parse(buffer.toString().trim()))
                    } catch (e: Exception) {
                        // Invalid message, skip
                    }
                }
                break
            }

            when (val char = current.toChar()) {
                '\r' -> {
                    buffer.append(char)
                    consecutiveLineBreaks++
                    lastWasCR = true

                    if (consecutiveLineBreaks == 2) {
                        emitMessage(buffer)
                    }
                }
                '\n' -> {
                    if (lastWasCR) {
                        buffer.append(char)
                        lastWasCR = false
                    } else {
                        buffer.append(char)
                        consecutiveLineBreaks++

                        if (consecutiveLineBreaks == 2) {
                            emitMessage(buffer)
                        }
                    }
                }
                else -> {
                    buffer.append(char)
                    consecutiveLineBreaks = 0
                    lastWasCR = false
                }
            }
        }
    }
}

private suspend fun SequenceScope<SseMessage>.emitMessage(buffer: StringBuilder) {
    val content = buffer.toString().trimEnd('\r', '\n')
    if (content.isNotEmpty()) {
        try {
            yield(SseMessage.parse(content))
        } catch (e: Exception) {
            // Invalid message, skip
        }
    }
    buffer.clear()
}
