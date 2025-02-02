package org.http4k.sse

import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.lens.accept
import org.http4k.mcp.client.internal.chunkedSseSequence
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class Http4kSseClient(private val http: HttpHandler) : AutoCloseable {

    private val running = AtomicBoolean(false)

    operator fun invoke(sseRequest: Request, onMessage: (SseMessage) -> Boolean) {
        running.set(true)
        thread {
            do {
                try {
                    val response = http(sseRequest.accept(TEXT_EVENT_STREAM))

                    when {
                        response.status.successful ->
                            response.body.stream.chunkedSseSequence().forEach { msg: SseMessage ->
                                if (!onMessage(msg)) return@thread
                            }

                        else -> error("Failed to connect to ${sseRequest.uri} ${response.status}")
                    }
                } catch (e: Exception) {
                    System.err.println("Error: $e")
                    e.printStackTrace()
                }
            } while (running.get())
        }
    }

    override fun close() {
        running.set(false)
    }
}
