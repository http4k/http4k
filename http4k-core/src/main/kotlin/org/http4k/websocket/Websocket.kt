package org.http4k.websocket

import org.http4k.core.Request
import org.http4k.core.Status
import java.io.Closeable
import java.util.concurrent.LinkedBlockingQueue

interface WebSocket : Closeable {
    operator fun invoke(message: WsMessage)
    fun onError(fn: (Throwable) -> Unit)
    fun onClose(fn: (Status) -> Unit)
    fun onMessage(fn: (WsMessage) -> Unit)

    companion object {
        operator fun invoke() = MemoryWebSocket()
    }
}

class MemoryWebSocket : WebSocket {
    override fun onError(fn: (Throwable) -> Unit) {
        TODO("not implemented")
    }

    override fun onClose(fn: (Status) -> Unit) {
        TODO("not implemented")
    }

    override fun onMessage(fn: (WsMessage) -> Unit) {
        TODO("not implemented")
    }

    private val queue = LinkedBlockingQueue<() -> WsMessage?>()

    val received = generateSequence { queue.take()() }

    override fun invoke(p1: WsMessage) {
        queue.add { p1 }
    }

    override fun close() {
        queue.add { null }
    }
}

typealias WsHandler = (WebSocket) -> Unit

typealias WsRouter = (Request) -> WsHandler?

class RoutingWsRouter(private vararg val list: WsRouter) : WsRouter {
    override fun invoke(p1: Request): WsHandler? = list.firstOrNull { it(p1) != null }?.invoke(p1)
}
