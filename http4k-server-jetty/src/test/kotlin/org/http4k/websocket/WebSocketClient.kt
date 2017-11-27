package org.http4k.websocket

import org.http4k.core.Status
import java.util.concurrent.LinkedBlockingQueue

class WebSocketClient(handler: WsHandler) : MutableInboundWebSocket() {

    private val queue = LinkedBlockingQueue<() -> WsMessage?>()

    val received = generateSequence { queue.take()() }

    init {
        handler(this)
        this.onClose {
            queue.add { null }
        }
    }

    override fun invoke(message: WsMessage): WebSocket = apply {
        queue.add { message }
    }

    override fun close() {
        triggerClose(Status(0, ""))
    }
}