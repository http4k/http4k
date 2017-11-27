package org.http4k.websocket

import org.http4k.core.Request
import org.http4k.core.Status
import java.util.concurrent.LinkedBlockingQueue

class TestingWsClient internal constructor(consumer: WsConsumer) : PullPushWebSocketAdapter() {

    private val queue = LinkedBlockingQueue<() -> WsMessage?>()

    val received = generateSequence { queue.take()() }

    init {
        consumer(this)
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

fun RoutingWsHandler.asClient(request: Request) = invoke(request)?.let(::TestingWsClient)!!
