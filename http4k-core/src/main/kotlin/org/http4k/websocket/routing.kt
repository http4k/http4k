package org.http4k.websocket

import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.UriTemplate
import java.io.Closeable

interface WebSocket : Closeable {
    operator fun invoke(message: WsMessage): WebSocket
    fun onError(fn: (Throwable) -> Unit): WebSocket
    fun onClose(fn: (Status) -> Unit): WebSocket
    fun onMessage(fn: (WsMessage) -> Unit): WebSocket
}

typealias WsHandler = (WebSocket) -> Unit

interface WsMatcher {
    fun match(request: Request): WsHandler?
}

interface RoutingWsMatcher : WsMatcher {
    fun withBasePath(new: String): RoutingWsMatcher
}

data class TemplatingRoutingWsMatcher(val template: UriTemplate,
                                      val router: WsHandler) : RoutingWsMatcher {
    override fun match(request: Request): WsHandler? = if (template.matches(request.uri.path)) router else null

    override fun withBasePath(new: String): TemplatingRoutingWsMatcher = copy(template = UriTemplate.from("$new/$template"))
}

fun websocket(vararg list: RoutingWsMatcher): RoutingWsMatcher = object : RoutingWsMatcher {
    override fun match(request: Request): WsHandler? = list.firstOrNull { it.match(request) != null }?.match(request)
    override fun withBasePath(new: String): RoutingWsMatcher = websocket(*list.map { it.withBasePath(new) }.toTypedArray())
}

infix fun String.bind(ws: WsHandler): RoutingWsMatcher = TemplatingRoutingWsMatcher(UriTemplate.from(this), ws)

infix fun String.bind(ws: RoutingWsMatcher): RoutingWsMatcher = ws.withBasePath(this)