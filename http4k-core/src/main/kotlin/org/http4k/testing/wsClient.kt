package org.http4k.testing

import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.websocket.PolyHandler
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage
import java.util.ArrayDeque

interface WsClient {
    val received: Sequence<WsMessage>
    fun close(status: Status)
    fun send(message: WsMessage)
}


data class ClosedWebsocket(val status: Status) : Exception()

class TestWsClient internal constructor(consumer: WsConsumer, request: Request) : WsClient {

    private val queue = ArrayDeque<() -> WsMessage?>()

    override val received = generateSequence { queue.remove()()!! }

    private val socket = object : PushPullAdaptingWebSocket(request) {
        init {
            consumer(this)
            onClose {
                queue.add { throw ClosedWebsocket(it) }
            }
        }

        override fun send(message: WsMessage) {
            queue.add { message }
        }

        override fun close(status: Status) {
            queue.add { throw ClosedWebsocket(status) }
        }
    }

    fun error(throwable: Throwable) = socket.triggerError(throwable)

    override fun close(status: Status) = socket.triggerClose(status)

    override fun send(message: WsMessage) = socket.triggerMessage(message)
}

fun WsHandler.testWsClient(request: Request): TestWsClient? = invoke(request)?.let { TestWsClient(it, request) }
fun PolyHandler.testWsClient(request: Request): TestWsClient? = ws.testWsClient(request)

