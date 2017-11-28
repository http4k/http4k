package org.http4k.websocket

import org.http4k.core.Request
import org.http4k.core.Status
import java.net.ConnectException
import java.util.concurrent.LinkedBlockingQueue

interface WsClient {
    val received: Sequence<WsMessage>
    operator fun invoke(throwable: Throwable)
    operator fun invoke(status: Status)
    operator fun invoke(message: WsMessage)
}

private class WsConsumerClient(consumer: WsConsumer, request: Request) : WsClient {

    private val queue = LinkedBlockingQueue<() -> WsMessage?>()

    override val received = generateSequence { queue.take()() }

    private val socket = object: PullPushAdaptingWebSocket(request) {
        init {
            consumer(this)
            onClose {
                queue.add { null }
            }
        }

        override fun send(message: WsMessage) = apply {
            queue.add { message }
        }

        override fun close(fn: Status): WebSocket = apply {
            queue.add { null }
        }
    }

    override fun invoke(throwable: Throwable) = socket.triggerError(throwable)

    override fun invoke(status: Status) = socket.triggerClose(status)

    override fun invoke(message: WsMessage) = socket.triggerMessage(message)

}

fun WsHandler.asClient(request: Request): WsClient = invoke(request)
    ?.let { WsConsumerClient(it, request) }
    ?: throw ConnectException("Could not find a websocket to bind to for this request")
