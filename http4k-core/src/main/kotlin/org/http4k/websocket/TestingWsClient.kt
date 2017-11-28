package org.http4k.websocket

import org.http4k.core.Request
import org.http4k.core.Status
import java.net.ConnectException
import java.util.concurrent.LinkedBlockingQueue

class TestingWsClient internal constructor(consumer: WsConsumer, upgradeRequest: Request) : PullPushAdaptingWebSocket(upgradeRequest) {

    private val queue = LinkedBlockingQueue<() -> WsMessage?>()

    val received = generateSequence { queue.take()() }

    init {
        consumer(this)
        onClose {
            queue.add { null }
        }
    }

    override fun send(message: WsMessage): TestingWsClient = apply {
        queue.add { message }
    }

    override fun close() {
        triggerClose(Status(0, ""))
    }
}

fun WsHandler.asClient(request: Request) = invoke(request)
    ?.let { TestingWsClient(it, request) }
    ?: throw ConnectException("Could not find a websocket to bind to for this request")
