package org.http4k.server

import org.http4k.websocket.WsMessage
import java.io.Closeable
import java.util.concurrent.LinkedBlockingQueue

interface Http4kWebSocket : Closeable, (WsMessage) -> Unit {
    companion object {
        operator fun invoke() = MemoryHttp4kWebSocket()
    }
}

class MemoryHttp4kWebSocket : Http4kWebSocket {

    private val queue = LinkedBlockingQueue<() -> WsMessage?>()

    val received = generateSequence { queue.take()() }

    override fun invoke(p1: WsMessage) {
        queue.add { p1 }
    }

    override fun close() {
        queue.add { null }
    }
}
