package org.http4k.sse

import com.launchdarkly.eventsource.EventSource
import com.launchdarkly.eventsource.MessageEvent
import com.launchdarkly.eventsource.background.BackgroundEventHandler
import com.launchdarkly.eventsource.background.BackgroundEventSource
import org.http4k.core.Uri
import java.net.URI
import java.util.concurrent.LinkedBlockingQueue

class BlockingSseClient(
    uri: Uri,
    private val queue: LinkedBlockingQueue<() -> SseMessage?> = LinkedBlockingQueue<() -> SseMessage?>()
) : SseClient {
    private val handler = Handler(queue)
    private val client = BackgroundEventSource.Builder(handler, EventSource.Builder(URI.create(uri.toString()))).build()

    init {
        client.start()
    }

    override fun received() = generateSequence { queue.take()() }

    override fun close() {
        client.close()
    }
}

private class Handler(private val queue: LinkedBlockingQueue<() -> SseMessage?>) : BackgroundEventHandler {
    override fun onOpen() {
    }

    override fun onClosed() {
        queue += { null }
    }

    override fun onMessage(event: String?, messageEvent: MessageEvent) {
        val sseEvent = when (event) {
            null, "message" -> SseMessage.Data(messageEvent.data)
            else -> SseMessage.Event(event, messageEvent.data, messageEvent.lastEventId)
        }
        queue += { sseEvent }
    }

    override fun onComment(comment: String) {
    }

    override fun onError(t: Throwable) {
        t.printStackTrace()
    }
}
