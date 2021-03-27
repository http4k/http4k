package org.http4k.testing

import org.http4k.core.Request
import org.http4k.server.PolyHandler
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.WsClient
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus
import org.http4k.websocket.WsStatus.Companion.NORMAL
import java.util.ArrayDeque

data class ClosedWebsocket(val status: WsStatus = NORMAL) : RuntimeException()

/**
 * A class that is used for *offline* testing of a routed Websocket, without starting up a Server. Calls
 * are routed synchronously to the receiving Websocket, and error are propagated to the caller.
 */
class TestWsClient internal constructor(consumer: WsConsumer, request: Request) : WsClient {

    private val queue = ArrayDeque<() -> WsMessage?>()

    override fun received() = generateSequence { queue.remove()()!! }

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

        override fun close(status: WsStatus) {
            queue.add { throw ClosedWebsocket(status) }
        }
    }

    /**
     * Push an error to the Websocket
     */
    fun error(throwable: Throwable) = socket.triggerError(throwable)

    override fun close(status: WsStatus) = socket.triggerClose(status)

    override fun send(message: WsMessage) = socket.triggerMessage(message)
}

fun WsHandler.testWsClient(request: Request): TestWsClient? = invoke(request)?.let { TestWsClient(it, request) }
fun PolyHandler.testWsClient(request: Request): TestWsClient? = ws?.testWsClient(request) ?: error("No WS handler set.")
