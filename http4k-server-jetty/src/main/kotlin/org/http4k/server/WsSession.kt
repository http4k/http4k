package org.http4k.server

import org.http4k.websocket.WsMessage
import org.http4k.websocket.string
import java.io.Closeable
import java.util.concurrent.LinkedBlockingQueue

interface WsSession : Closeable, (WsMessage) -> Unit {
    companion object {
        operator fun invoke() = MemoryWsSession()
    }
}

class MemoryWsSession : WsSession {

    private val queue = LinkedBlockingQueue<() -> WsMessage?>()

    val received = generateSequence { queue.take()() }

    override fun invoke(p1: WsMessage) {
        queue.add { p1 }
    }

    override fun close() {
        queue.add { null }
    }
}
