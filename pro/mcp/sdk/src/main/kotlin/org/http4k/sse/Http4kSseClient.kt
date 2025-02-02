package org.http4k.sse

import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.lens.accept
import org.http4k.mcp.client.internal.chunkedSseSequence
import java.time.Duration
import java.time.Duration.ofSeconds
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class Http4kSseClient(
    private val http: HttpHandler,
    private val reconnectionDelay: Duration = ofSeconds(1)
) : AutoCloseable {

    private val running = AtomicBoolean(false)

    operator fun invoke(sseRequest: Request, onMessage: (SseMessage) -> Boolean) {
        running.set(true)
        thread {
            do {
                try {
                    val response = http(sseRequest.accept(TEXT_EVENT_STREAM))

                    when {
                        response.status.successful ->
                            response.body.stream.chunkedSseSequence().forEach {
                                if (!onMessage(it)) return@thread
                            }

                        else -> error("Failed to connect to ${sseRequest.uri} ${response.status}")
                    }
                } catch (e: Exception) {
                    Thread.sleep(reconnectionDelay)
                }
            } while (running.get())
        }
    }

    override fun close() {
        running.set(false)
    }
}
