package org.http4k.websocket

import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.UriTemplate
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

interface WsRouter {
    fun match(request: Request): WsHandler?
}

interface SomethingWsRouter : WsRouter {
    fun withBasePath(new: String): SomethingWsRouter
}

data class TemplatingSomethingWsRouter(val template: UriTemplate,
                                       val router: WsHandler) : SomethingWsRouter {
    override fun match(request: Request): WsHandler? {
        return if (template.matches(request.uri.path)) router else null
    }

    override fun withBasePath(new: String): TemplatingSomethingWsRouter = copy(template = UriTemplate.from("$new/$template"))
}

infix fun String.bind(ws: WsHandler): SomethingWsRouter = TemplatingSomethingWsRouter(UriTemplate.from(this), ws)

infix fun String.bind(ws: SomethingWsRouter): SomethingWsRouter = ws.withBasePath(this)

fun websocket(vararg list: SomethingWsRouter): SomethingWsRouter = object : SomethingWsRouter {
    override fun match(request: Request): WsHandler? = list.firstOrNull { it.match(request) != null }?.match(request)
    override fun withBasePath(new: String): SomethingWsRouter = websocket(*list.map { it.withBasePath(new) }.toTypedArray())
}