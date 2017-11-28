package org.http4k.websocket

import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.UriTemplate
import java.io.Closeable

interface WebSocket : Closeable {
    val upgradeRequest: Request
    fun send(message: WsMessage): WebSocket
    fun onError(fn: (Throwable) -> Unit): WebSocket
    fun onClose(fn: (Status) -> Unit): WebSocket
    fun onMessage(fn: (WsMessage) -> Unit): WebSocket
}

typealias WsConsumer = (WebSocket) -> Unit

interface WsHandler {
    operator fun invoke(request: Request): WsConsumer?
}

fun websocket(vararg list: RoutingWsHandler): RoutingWsHandler = object : RoutingWsHandler {
    override operator fun invoke(request: Request): WsConsumer? = list.firstOrNull { it.invoke(request) != null }?.invoke(request)
    override fun withBasePath(new: String): RoutingWsHandler = websocket(*list.map { it.withBasePath(new) }.toTypedArray())
}

interface RoutingWsHandler : WsHandler {
    fun withBasePath(new: String): RoutingWsHandler
}

data class TemplatingRoutingWsHandler(val template: UriTemplate,
                                      val router: WsConsumer) : RoutingWsHandler {
    override operator fun invoke(request: Request): WsConsumer? = if (template.matches(request.uri.path)) router else null

    override fun withBasePath(new: String): TemplatingRoutingWsHandler = copy(template = UriTemplate.from("$new/$template"))
}

infix fun String.bind(ws: WsConsumer): RoutingWsHandler = TemplatingRoutingWsHandler(UriTemplate.from(this), ws)

infix fun String.bind(ws: RoutingWsHandler): RoutingWsHandler = ws.withBasePath(this)