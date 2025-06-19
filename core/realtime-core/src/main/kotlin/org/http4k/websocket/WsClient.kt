package org.http4k.websocket

import org.http4k.websocket.WsStatus.Companion.NORMAL
import java.util.concurrent.LinkedBlockingQueue

interface WsClient : AutoCloseable {
    fun received(): Sequence<WsMessage>
    fun close(status: WsStatus)
    fun send(message: WsMessage)
    override fun close() = close(NORMAL)
}

fun Websocket.toWsClient(): WsClient {
    val inner = this
    val queue = LinkedBlockingQueue<() -> WsMessage?>()

    onMessage { queue += { it } }
    onError { queue += { throw it } }
    onClose { queue += { null } }

    return object : WsClient {
        override fun received() = generateSequence { queue.take()() }
        override fun close(status: WsStatus) = inner.close(status)
        override fun send(message: WsMessage) = inner.send(message)
    }
}
