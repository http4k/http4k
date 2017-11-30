package org.http4k.testing

import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.websocket.PolyHandler
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage
import java.util.concurrent.LinkedBlockingQueue

interface WsClient {
    val received: Sequence<WsMessage>
    fun error(throwable: Throwable)
    fun close(status: Status)
    fun send(message: WsMessage)
}

private class WsConsumerClient(consumer: WsConsumer, request: Request) : WsClient {

    private val queue = LinkedBlockingQueue<() -> WsMessage?>()

    override val received = generateSequence { queue.take()() }

    private val socket = object : PushPullAdaptingWebSocket(request) {
        init {
            consumer(this)
            onClose {
                queue.add { null }
            }
        }

        override fun send(message: WsMessage) {
            queue.add { message }
        }

        override fun close(status: Status) {
            queue.add { null }
        }
    }

    override fun error(throwable: Throwable) = socket.triggerError(throwable)

    override fun close(status: Status) = socket.triggerClose(status)

    override fun send(message: WsMessage) = socket.triggerMessage(message)
}

fun WsHandler.testWsClient(request: Request): WsClient? = invoke(request)?.let { WsConsumerClient(it, request) }
fun PolyHandler.testWsClient(request: Request) = ws.testWsClient(request)

