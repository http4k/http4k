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

interface RoutingWsHandler : WsHandler {
    fun withBasePath(new: String): RoutingWsHandler
}

data class TemplatingRoutingWsHandler(val template: UriTemplate,
                                      val router: WsConsumer) : RoutingWsHandler {
    override operator fun invoke(request: Request): WsConsumer? = if (template.matches(request.uri.path)) router else null

    override fun withBasePath(new: String): TemplatingRoutingWsHandler = copy(template = UriTemplate.from("$new/$template"))
}
