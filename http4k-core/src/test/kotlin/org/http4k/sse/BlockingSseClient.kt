package org.http4k.sse

import com.launchdarkly.eventsource.EventHandler
import com.launchdarkly.eventsource.EventSource.Builder
import com.launchdarkly.eventsource.MessageEvent
import org.http4k.core.Uri
import java.net.URI.create
import java.util.concurrent.LinkedBlockingQueue

class BlockingSseClient(
    uri: Uri,
    private val queue: LinkedBlockingQueue<() -> SseMessage?> = LinkedBlockingQueue<() -> SseMessage?>()
) : SseClient {
    private val handler = Handler(queue)
    private val client = Builder(handler, create(uri.toString())).build()

    init {
        client.start()
    }

    override fun received() = generateSequence { queue.take()() }

    override fun close() {
        client.close()
    }
}

private class Handler(private val queue: LinkedBlockingQueue<() -> SseMessage?>) : EventHandler {
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
